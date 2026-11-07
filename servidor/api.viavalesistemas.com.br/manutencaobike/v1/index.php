<?php

declare(strict_types=1);

// Procura a pasta privada subindo a partir daqui, para funcionar com qualquer raiz de subdomínio.
$pasta = __DIR__;
while (!is_file("$pasta/manutencaobike_privado/bootstrap.php")) {
    $acima = dirname($pasta);
    if ($acima === $pasta) {
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
        echo '{"erro":"Servidor não configurado."}';
        exit;
    }
    $pasta = $acima;
}

require "$pasta/manutencaobike_privado/bootstrap.php";

atenderRequisicao();
