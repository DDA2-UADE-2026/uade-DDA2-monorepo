import { describe, expect, it } from "vitest"

import { MAX_PROGRAM_IMAGE_BYTES, programImageSource, validateProgramImageFile } from "./program-images"

describe.skip("imágenes de portada de programas", () => {
  it("acepta imágenes JPG y PNG dentro del límite", () => {
    expect(validateProgramImageFile({ name: "portada.jpg", type: "image/jpeg", size: 1024 })).toBeUndefined()
    expect(validateProgramImageFile({ name: "portada.png", type: "image/png", size: MAX_PROGRAM_IMAGE_BYTES })).toBeUndefined()
  })

  it("rechaza archivos vacíos, demasiado grandes o de otro formato", () => {
    expect(validateProgramImageFile({ name: "portada.png", type: "image/png", size: 0 })).toMatch(/vacía/)
    expect(validateProgramImageFile({ name: "portada.png", type: "image/png", size: MAX_PROGRAM_IMAGE_BYTES + 1 })).toMatch(/10 MB/)
    expect(validateProgramImageFile({ name: "portada.pdf", type: "application/pdf", size: 100 })).toMatch(/JPG o PNG/)
    expect(validateProgramImageFile({ name: "portada.jpg", type: "image/png", size: 100 })).toMatch(/formato correcto/)
  })

  it("combina las rutas públicas con la URL configurada del backend", () => {
    expect(programImageSource("/api/images/image-1")).toBe("/proxy/api/images/image-1")
    expect(programImageSource("https://cdn.example.com/portada.png")).toBe("https://cdn.example.com/portada.png")
    expect(programImageSource()).toBeUndefined()
  })
})
