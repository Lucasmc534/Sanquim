package br.com.sanquim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/** Inicializa a API escolar e os componentes Spring. */
public class SanquimApplication {
    public static void main(String[] args) {
        SpringApplication.run(SanquimApplication.class, args);
    }
}
