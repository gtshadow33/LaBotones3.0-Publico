package com.labotones.Class_help;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class Button_prop {

    private int    id;
    private String name;
    private String description;
    private String locateBat;  // ← campo
    private String icono;
    private boolean argumento;
    private boolean excluidoBloqueo; // ← NUEVO: si true, el botón nunca se deshabilita por el bloqueo
    private String categoria; // ← NUEVO: categoría para organizar/filtrar los botones
    private boolean requiereConfirmacion; // ← NUEVO: si true, pide confirmación con cuenta atrás antes de ejecutar

    /** Categoría por defecto cuando no se especifica ninguna */
    public static final String CATEGORIA_DEFECTO = "General";

    public Button_prop() {}

    @JsonCreator
    public Button_prop(
            @JsonProperty("id")           int     id,
            @JsonProperty("name")        String  name,
            @JsonProperty("description") String  description,
            @JsonProperty("locateBat")    String  locateBat,  // ← AHORA usa "locateBat"
            @JsonProperty("icono")       String  icono,
            @JsonProperty("argumento")   boolean argumento,
            @JsonProperty("excluidoBloqueo") boolean excluidoBloqueo,
            @JsonProperty("categoria") String categoria,
            @JsonProperty("requiereConfirmacion") boolean requiereConfirmacion) {

        this.id          = id;
        this.name        = Objects.requireNonNullElse(name, "").trim();
        this.description = Objects.requireNonNullElse(description, "").trim();
        this.locateBat   = Objects.requireNonNullElse(locateBat, "").trim();
        this.icono       = Objects.requireNonNullElse(icono, "").trim();
        this.argumento   = argumento;
        this.excluidoBloqueo = excluidoBloqueo;
        String catLimpia = Objects.requireNonNullElse(categoria, "").trim();
        this.categoria   = catLimpia.isEmpty() ? CATEGORIA_DEFECTO : catLimpia;
        this.requiereConfirmacion = requiereConfirmacion;
    }

    // VALIDACIÓN
    @JsonIgnore  // ← Para que no aparezca "valid" en el JSON
    public boolean isValid() {
        return name != null && !name.isBlank()
            && locateBat != null && !locateBat.isBlank();
    }

    // GETTERS / SETTERS
    @JsonProperty("id")
    public int getID() { return id; }
    public void setID(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String v) { this.name = v == null ? "" : v.trim(); }

    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v == null ? "" : v.trim(); }

    @JsonProperty("locateBat")  // ← AHORA guarda como "locateBat"
    public String getLocateBat() { return locateBat; }
    public void setLocateBat(String v) { this.locateBat = v == null ? "" : v.trim(); }

    public String getIcono() { return icono; }
    public void setIcono(String v) { this.icono = v == null ? "" : v.trim(); }

    public boolean isArgumento() { return argumento; }
    public void setArgumento(boolean v) { this.argumento = v; }

    public boolean isExcluidoBloqueo() { return excluidoBloqueo; }
    public void setExcluidoBloqueo(boolean v) { this.excluidoBloqueo = v; }

    public String getCategoria() { return categoria == null || categoria.isBlank() ? CATEGORIA_DEFECTO : categoria; }
    public void setCategoria(String v) { this.categoria = (v == null || v.isBlank()) ? CATEGORIA_DEFECTO : v.trim(); }

    public boolean isRequiereConfirmacion() { return requiereConfirmacion; }
    public void setRequiereConfirmacion(boolean v) { this.requiereConfirmacion = v; }

    // COPIA
    public Button_prop copy() {
        Button_prop copy = new Button_prop();
        copy.setID(id);
        copy.setName(name);
        copy.setDescription(description);
        copy.setLocateBat(locateBat);
        copy.setIcono(icono);
        copy.setArgumento(argumento);
        copy.setExcluidoBloqueo(excluidoBloqueo);
        copy.setCategoria(categoria);
        copy.setRequiereConfirmacion(requiereConfirmacion);
        return copy;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + name;
    }
}