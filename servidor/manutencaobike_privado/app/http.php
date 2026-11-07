<?php

declare(strict_types=1);

/** Erro que vira resposta JSON com o status e a mensagem informados. */
final class ErroHttp extends RuntimeException
{
    public function __construct(public readonly int $status, string $mensagem)
    {
        parent::__construct($mensagem);
    }
}

function config(string $chave): mixed
{
    static $config = null;
    if ($config === null) {
        $arquivo = RAIZ_PRIVADA . '/config.php';
        if (!is_file($arquivo)) {
            throw new RuntimeException('config.php não encontrado em ' . RAIZ_PRIVADA);
        }
        $config = require $arquivo;
    }
    if (!array_key_exists($chave, $config)) {
        throw new RuntimeException("Configuração ausente: $chave");
    }
    return $config[$chave];
}

function responderJson(int $status, array $corpo): never
{
    http_response_code($status);
    header('Content-Type: application/json; charset=utf-8');
    header('Cache-Control: no-store');
    echo json_encode($corpo, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit;
}

function lerCorpoJson(): array
{
    $bruto = file_get_contents('php://input', length: 64 * 1024);
    $dados = json_decode($bruto === false ? '' : $bruto, true);
    if (!is_array($dados)) {
        throw new ErroHttp(400, 'JSON inválido.');
    }
    return $dados;
}

/** Token de sessão do cabeçalho "Authorization: Bearer <token>". */
function tokenDoCabecalho(): ?string
{
    // Em CGI/FPM o Apache costuma esconder o Authorization; o .htaccess o repassa como variável.
    $cabecalho = $_SERVER['HTTP_AUTHORIZATION'] ?? $_SERVER['REDIRECT_HTTP_AUTHORIZATION'] ?? '';
    if ($cabecalho === '' && function_exists('apache_request_headers')) {
        foreach (apache_request_headers() as $nome => $valor) {
            if (strcasecmp($nome, 'Authorization') === 0) {
                $cabecalho = $valor;
            }
        }
    }
    return preg_match('/^Bearer\s+([a-f0-9]{64})$/i', trim($cabecalho), $m) ? strtolower($m[1]) : null;
}
