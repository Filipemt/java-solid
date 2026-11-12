package com.banco.pj.infra;

public final class ValidadorCnpj {

    private ValidadorCnpj() {
    }

    public static String somenteDigitos(String valor) {
        return valor == null ? null : valor.replaceAll("\\D", "");
    }

    public static boolean isValido(String cnpj) {
        if (cnpj == null) {
            return false;
        }
        String d = somenteDigitos(cnpj);
        if (d.length() != 14) {
            return false;
        }
        if (d.chars().distinct().count() == 1) {
            return false;
        }
        int dv1 = digito(d, 12, new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        int dv2 = digito(d, 13, new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return dv1 == d.charAt(12) - '0' && dv2 == d.charAt(13) - '0';
    }

    private static int digito(String d, int quantidade, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            soma += (d.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
