package br.controle.projetor2;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int INTERVALO_REPETICAO_MS = 130;
    private static final int INTERVALO_BUSCA_MS = 900;

    private ConsumerIrManager ir;
    private boolean temIr;
    private SharedPreferences prefs;
    private HandlerThread fio;
    private Handler envio;
    private final Handler tela = new Handler(Looper.getMainLooper());

    private int perfilAtual;
    private volatile int teclaSegurada = -1;
    private long ultimoAviso;

    private boolean buscando;
    private int comandoBusca = -1;

    private TextView txtPerfil, txtBusca, txtAviso, btnBusca;
    private View painelAjustes;

    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        setContentView(R.layout.main);

        prefs = getSharedPreferences("controle", MODE_PRIVATE);
        perfilAtual = prefs.getInt("perfil", 0);
        if (perfilAtual < 0 || perfilAtual >= Perfis.LISTA.length) perfilAtual = 0;

        try {
            ir = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
            temIr = ir != null && ir.hasIrEmitter();
        } catch (Exception e) {
            temIr = false;
        }

        fio = new HandlerThread("infravermelho");
        fio.start();
        envio = new Handler(fio.getLooper());

        txtPerfil = (TextView) findViewById(R.id.txt_perfil);
        txtBusca = (TextView) findViewById(R.id.txt_busca);
        txtAviso = (TextView) findViewById(R.id.txt_aviso);
        btnBusca = (TextView) findViewById(R.id.btn_busca);
        painelAjustes = findViewById(R.id.painel_ajustes);

        if (!temIr) {
            txtAviso.setText(R.string.sem_ir);
            txtAviso.setVisibility(View.VISIBLE);
        }

        tecla(R.id.btn_power, Perfis.POWER, false);
        tecla(R.id.btn_source, Perfis.SOURCE, false);
        tecla(R.id.btn_up, Perfis.UP, true);
        tecla(R.id.btn_down, Perfis.DOWN, true);
        tecla(R.id.btn_left, Perfis.LEFT, true);
        tecla(R.id.btn_right, Perfis.RIGHT, true);
        tecla(R.id.btn_ok, Perfis.OK, false);
        tecla(R.id.btn_back, Perfis.BACK, false);
        tecla(R.id.btn_home, Perfis.HOME, false);
        tecla(R.id.btn_play, Perfis.PLAY, false);
        tecla(R.id.btn_menu, Perfis.MENU, false);
        tecla(R.id.btn_mute, Perfis.MUTE, false);
        tecla(R.id.btn_vol_up, Perfis.VOL_UP, true);
        tecla(R.id.btn_vol_down, Perfis.VOL_DOWN, true);
        tecla(R.id.btn_foco_mais, Perfis.FOCUS_PLUS, true);
        tecla(R.id.btn_foco_menos, Perfis.FOCUS_MINUS, true);

        clique(R.id.btn_ajustes, new Runnable() { public void run() {
            boolean abrir = painelAjustes.getVisibility() != View.VISIBLE;
            painelAjustes.setVisibility(abrir ? View.VISIBLE : View.GONE);
            if (!abrir) pararBusca();
        }});
        clique(R.id.btn_perfil_ant, new Runnable() { public void run() { trocarPerfil(-1); }});
        clique(R.id.btn_perfil_prox, new Runnable() { public void run() { trocarPerfil(1); }});
        clique(R.id.btn_busca, new Runnable() { public void run() {
            if (buscando) pararBusca(); else iniciarBusca();
        }});
        clique(R.id.btn_busca_ant, new Runnable() { public void run() { passoBusca(-1); }});
        clique(R.id.btn_busca_prox, new Runnable() { public void run() { passoBusca(1); }});
        clique(R.id.btn_busca_repetir, new Runnable() { public void run() { passoBusca(0); }});
        clique(R.id.btn_salvar_mais, new Runnable() { public void run() { salvarFoco(Perfis.FOCUS_PLUS); }});
        clique(R.id.btn_salvar_menos, new Runnable() { public void run() { salvarFoco(Perfis.FOCUS_MINUS); }});
        clique(R.id.btn_apagar_foco, new Runnable() { public void run() { apagarFoco(); }});

        mostrarPerfil();
        mostrarBusca();
    }

    @Override
    protected void onPause() {
        super.onPause();
        teclaSegurada = -1;
        pararBusca();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fio.quit();
    }

    // ---------- botoes do controle ----------

    private void clique(int id, final Runnable acao) {
        findViewById(id).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { acao.run(); }
        });
    }

    private void tecla(int id, final int tecla, final boolean repete) {
        findViewById(id).setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                int acao = e.getActionMasked();
                if (acao == MotionEvent.ACTION_DOWN) {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    apertar(tecla, repete);
                } else if (acao == MotionEvent.ACTION_UP || acao == MotionEvent.ACTION_CANCEL) {
                    if (teclaSegurada == tecla) teclaSegurada = -1;
                }
                return false;
            }
        });
    }

    /** Codigo da tecla no perfil atual; o foco salvo pela busca tem prioridade. */
    private int codigoDe(int tecla) {
        int salvo = prefs.getInt(chaveFoco(perfilAtual, tecla), -1);
        if (salvo >= 0) return salvo;
        return Perfis.LISTA[perfilAtual].codigos[tecla];
    }

    private static String chaveFoco(int perfil, int tecla) {
        return "p" + perfil + "_t" + tecla;
    }

    private void apertar(final int tecla, boolean repete) {
        pararBusca();
        if (!temIr) {
            avisar(getString(R.string.sem_ir));
            return;
        }
        int codigo = codigoDe(tecla);
        if (codigo < 0) {
            boolean foco = tecla == Perfis.FOCUS_PLUS || tecla == Perfis.FOCUS_MINUS;
            avisar(getString(foco ? R.string.sem_foco : R.string.sem_botao));
            return;
        }
        final int[] sinal = Nec.sinal(Perfis.LISTA[perfilAtual], codigo);
        teclaSegurada = repete ? tecla : -1;
        envio.removeCallbacksAndMessages(null);
        envio.post(new Runnable() {
            public void run() {
                transmitir(sinal);
                if (teclaSegurada == tecla) envio.postDelayed(this, INTERVALO_REPETICAO_MS);
            }
        });
    }

    private void transmitir(int[] sinal) {
        try {
            ir.transmit(Nec.FREQUENCIA, sinal);
        } catch (Exception e) {
            // aparelho recusou o envio; nao ha o que fazer alem de nao travar o app
        }
    }

    private void avisar(String texto) {
        long agora = System.currentTimeMillis();
        if (agora - ultimoAviso < 2500) return;
        ultimoAviso = agora;
        Toast.makeText(this, texto, Toast.LENGTH_SHORT).show();
    }

    // ---------- escolha do codigo (perfil) ----------

    private void trocarPerfil(int passo) {
        pararBusca();
        int n = Perfis.LISTA.length;
        perfilAtual = (perfilAtual + passo + n) % n;
        prefs.edit().putInt("perfil", perfilAtual).apply();
        comandoBusca = -1;
        mostrarPerfil();
        mostrarBusca();
    }

    private void mostrarPerfil() {
        txtPerfil.setText(getString(R.string.perfil_atual,
                perfilAtual + 1, Perfis.LISTA.length, Perfis.LISTA[perfilAtual].nome));
    }

    // ---------- busca do codigo de foco ----------

    private final Runnable tiqueBusca = new Runnable() {
        public void run() {
            if (!buscando) return;
            if (!avancarBusca(1)) {
                pararBusca();
                txtBusca.setText(R.string.busca_fim);
                return;
            }
            enviarBusca();
            tela.postDelayed(this, INTERVALO_BUSCA_MS);
        }
    };

    /** Comandos que o perfil ja usa em outro botao nao precisam ser testados (ex.: desligar). */
    private boolean comandoJaUsado(int comando) {
        int[] codigos = Perfis.LISTA[perfilAtual].codigos;
        for (int t = 0; t < codigos.length; t++) {
            if (t == Perfis.FOCUS_PLUS || t == Perfis.FOCUS_MINUS) continue;
            if (codigos[t] >= 0 && (codigos[t] & 0xFF) == comando) return true;
        }
        return false;
    }

    private boolean avancarBusca(int passo) {
        int c = comandoBusca;
        do {
            c += passo;
            if (c < 0 || c > 255) return false;
        } while (comandoJaUsado(c));
        comandoBusca = c;
        return true;
    }

    private void enviarBusca() {
        if (comandoBusca < 0) return;
        final int[] sinal = Nec.sinal(Perfis.LISTA[perfilAtual], Nec.codigoSimples(comandoBusca));
        envio.post(new Runnable() { public void run() { transmitir(sinal); }});
        mostrarBusca();
    }

    private void iniciarBusca() {
        if (!temIr) {
            avisar(getString(R.string.sem_ir));
            return;
        }
        teclaSegurada = -1;
        buscando = true;
        btnBusca.setText(R.string.busca_pausar);
        tela.removeCallbacks(tiqueBusca);
        tela.post(tiqueBusca);
    }

    private void pararBusca() {
        if (!buscando) return;
        buscando = false;
        tela.removeCallbacks(tiqueBusca);
        btnBusca.setText(R.string.busca_iniciar);
    }

    /** passo 0 repete o comando atual. */
    private void passoBusca(int passo) {
        pararBusca();
        if (!temIr) {
            avisar(getString(R.string.sem_ir));
            return;
        }
        if (passo != 0 || comandoBusca < 0) {
            if (!avancarBusca(passo == 0 ? 1 : passo)) return;
        }
        enviarBusca();
    }

    private void mostrarBusca() {
        if (comandoBusca < 0) {
            txtBusca.setText(R.string.busca_parada);
        } else {
            txtBusca.setText(getString(R.string.busca_testando, comandoBusca));
        }
    }

    private void salvarFoco(int tecla) {
        pararBusca();
        if (comandoBusca < 0) {
            avisar(getString(R.string.busca_nada));
            return;
        }
        prefs.edit().putInt(chaveFoco(perfilAtual, tecla), Nec.codigoSimples(comandoBusca)).apply();
        Toast.makeText(this, getString(tecla == Perfis.FOCUS_PLUS
                ? R.string.salvo_mais : R.string.salvo_menos, comandoBusca), Toast.LENGTH_SHORT).show();
    }

    private void apagarFoco() {
        prefs.edit()
                .remove(chaveFoco(perfilAtual, Perfis.FOCUS_PLUS))
                .remove(chaveFoco(perfilAtual, Perfis.FOCUS_MINUS))
                .apply();
        Toast.makeText(this, R.string.foco_apagado, Toast.LENGTH_SHORT).show();
    }
}
