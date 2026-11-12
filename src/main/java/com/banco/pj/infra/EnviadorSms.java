package com.banco.pj.infra;

import java.util.ArrayList;
import java.util.List;

public class EnviadorSms {

    private final List<String> enviados = new ArrayList<>();

    public void enviar(String telefone, String mensagem) {
        System.out.println("[SMS] para=" + telefone + " msg=" + mensagem);
        enviados.add(telefone + "|" + mensagem);
    }

    public List<String> getEnviados() {
        return enviados;
    }
}
