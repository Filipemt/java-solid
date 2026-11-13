package com.banco.pj.dominio;

import java.util.ArrayList;
import java.util.List;

public class Auditoria {
    private final List<String> auditorias = new ArrayList<>();

    public void registrar(String auditoria) {
        auditorias.add(auditoria);
    }

    public List<String> getAuditorias() {
        return auditorias;
    }
}
