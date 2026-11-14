#!/usr/bin/env python3
"""Desenha o icone do app (PNG 192x192)."""
import sys
from PIL import Image, ImageDraw

S = 4  # desenha maior e reduz, para suavizar as bordas
T = 192 * S
img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rounded_rectangle([0, 0, T - 1, T - 1], radius=44 * S, fill="#151517")
d.rounded_rectangle([64 * S, 22 * S, 128 * S, 170 * S], radius=30 * S, fill="#3A3A40")
d.ellipse([86 * S, 36 * S, 106 * S, 56 * S], fill="#FF453A")
d.ellipse([74 * S, 72 * S, 118 * S, 116 * S], fill="#E6E6EA")
d.ellipse([88 * S, 86 * S, 104 * S, 102 * S], fill="#3A3A40")
d.rounded_rectangle([82 * S, 130 * S, 110 * S, 140 * S], radius=5 * S, fill="#8E8E96")
img.resize((192, 192), Image.LANCZOS).save(sys.argv[1])
