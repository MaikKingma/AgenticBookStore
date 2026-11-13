---
name: pdf-asset-extraction-technique
description: Extracting a logo/image with alpha from a PDF on a Mac without installs (stdlib python zlib on XObject + SMask)
applies-to:
  - guide/static/**
  - frontend/public/**
  - frontend/src/assets/**
metadata:
  type: reference
---

Before render-and-key (sips/Swift), inspect the PDF: inflate the page content stream with python3 `-I` + `zlib`
and look for `/ImN Do`. Embedded raster images often carry an `/SMask` (DeviceGray stream = true anti-aliased alpha),
so pair the inflated RGB stream with the inflated SMask and write RGBA PNG with struct+zlib (no PIL needed).
Recolour by constant RGB + the same alpha. Locate objects with `re` on `\nN 0 obj <<...>> stream` (works when the
PDF has no object streams). PIL, magick, pdftoppm, mutool, gs are absent on this Mac; sips, swift, node, qlmanage exist.
Native image size is the quality ceiling; report it against the displayed size.
