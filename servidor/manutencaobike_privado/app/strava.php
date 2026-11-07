<?php

declare(strict_types=1);

/** Chama o oauth/token do Strava com o client_secret, que só existe aqui no servidor. */
function stravaOauthToken(array $campos): array
{
    $campos['client_id'] = (string) config('strava_client_id');
    $campos['client_secret'] = (string) config('strava_client_secret');

    $curl = curl_init('https://www.strava.com/oauth/token');
    curl_setopt_array($curl, [
        CURLOPT_POST => true,
        CURLOPT_POSTFIELDS => http_build_query($campos),
        CURLOPT_RETURNTRANSFER => true,
        CURLOPT_CONNECTTIMEOUT => 10,
        CURLOPT_TIMEOUT => 20,
    ]);
    $corpo = curl_exec($curl);
    $status = (int) curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
    curl_close($curl);

    if ($corpo === false) {
        throw new ErroHttp(502, 'Não foi possível falar com o Strava.');
    }
    if ($status === 400 || $status === 401) {
        throw new ErroHttp(401, 'Autorização do Strava inválida ou expirada. Conecte novamente.');
    }
    $dados = json_decode((string) $corpo, true);
    if ($status !== 200 || !is_array($dados) || !isset($dados['access_token'], $dados['refresh_token'], $dados['expires_at'])) {
        throw new ErroHttp(502, 'Resposta inesperada do Strava.');
    }
    return $dados;
}

/** Só o que o app precisa; o resto da resposta do Strava não sai do servidor. */
function tokenParaApp(array $dados): array
{
    $token = [
        'access_token' => (string) $dados['access_token'],
        'refresh_token' => (string) $dados['refresh_token'],
        'expires_at' => (int) $dados['expires_at'],
    ];
    if (isset($dados['athlete']) && is_array($dados['athlete'])) {
        $token['athlete'] = [
            'firstname' => $dados['athlete']['firstname'] ?? null,
            'lastname' => $dados['athlete']['lastname'] ?? null,
        ];
    }
    return $token;
}
