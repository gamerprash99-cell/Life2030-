#!/usr/bin/env python3
"""Decode the generated Stitch screens with stdlib only and report their real
colour palettes, so the design can be judged without an image viewer."""
import zlib, struct, sys, os, glob
from collections import Counter

def read_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n'
    pos, idat, ihdr = 8, [], None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos+4])[0]
        typ = d[pos+4:pos+8]
        data = d[pos+8:pos+8+ln]
        if typ == b'IHDR':
            ihdr = struct.unpack('>IIBBBBB', data)
        elif typ == b'IDAT':
            idat.append(data)
        elif typ == b'IEND':
            break
        pos += 12 + ln
    w, h, bd, ct, comp, filt, inter = ihdr
    assert ct == 2 and bd == 8 and inter == 0, (ct, bd, inter)
    raw = zlib.decompress(b''.join(idat))
    bpp = 3
    stride = w * bpp
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p+stride]); p += stride
        if f == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i-bpp]) & 0xFF
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif f == 3:
            for i in range(stride):
                a = line[i-bpp] if i >= bpp else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif f == 4:
            for i in range(stride):
                a = line[i-bpp] if i >= bpp else 0
                b = prev[i]
                c = prev[i-bpp] if i >= bpp else 0
                pa, pb, pc = abs(b-c), abs(a-c), abs(a+b-2*c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        out[y*stride:(y+1)*stride] = line
        prev = line
    return w, h, out

def hexs(t):
    return '#%02X%02X%02X' % t

def quant(t, step=16):
    return tuple((v // step) * step + step // 2 for v in t)

def palette(path, topn=10):
    w, h, buf = read_png(path)
    stride = w * 3
    c = Counter()
    # sample a grid of pixels
    for y in range(0, h, max(1, h // 240)):
        base = y * stride
        for x in range(0, w, max(1, w // 160)):
            i = base + x * 3
            px = (buf[i], buf[i+1], buf[i+2])
            c[quant(px)] += 1
    total = sum(c.values())
    lines = []
    for t, n in c.most_common(topn):
        lines.append("%s  %5.1f%%" % (hexs(t), 100.0*n/total))
    return w, h, lines

if __name__ == '__main__':
    paths = sys.argv[1:] or sorted(glob.glob(os.path.join(os.path.dirname(__file__), 'screens', '*.png')))
    for p in paths:
        try:
            w, h, pal = palette(p)
            print("== %-16s %dx%d" % (os.path.basename(p), w, h))
            for l in pal:
                print("   ", l)
        except Exception as e:
            print("== %s ERROR %s" % (os.path.basename(p), e))
