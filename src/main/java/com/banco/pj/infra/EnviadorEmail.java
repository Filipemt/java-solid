package com.banco.pj.infra;

import java.util.ArrayList;
import java.util.List;

public class EnviadorEmail {

    private final List<String> enviados = new ArrayList<>();

    public void enviar(String destino, String assunto, String corpo) {
        System.out.println("[EMAIL] para=" + destino + " assunto=" + assunto + " corpo=" + corpo);
        enviados.add(destino + "|" + assunto);
    }

    public List<String> getEnviados() {
        return enviados;
    }
}
