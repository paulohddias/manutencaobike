<?php

declare(strict_types=1);

/*
 * O usuário é identificado pelo id de atleta do Strava. Ao conectar, o servidor gera um token de
 * sessão aleatório; só o hash SHA-256 dele fica em disco, então um vazamento da pasta de dados
 * não permite se passar por ninguém.
 */

function pastaUsuario(int $atletaId): string
{
    return caminhoDados('usuarios', (string) $atletaId);
}

function criarSessao(int $atletaId): string
{
    $token = bin2hex(random_bytes(32));
    $hash = hash('sha256', $token);
    $agora = time();

    gravarJson(caminhoDados('sessoes', "$hash.json"), ['atleta_id' => $atletaId, 'criada_em' => $agora]);
    // Índice reverso: permite apagar todas as sessões do usuário ao excluir a conta.
    touch(garantirPasta(pastaUsuario($atletaId) . '/sessoes') . "/$hash");

    $perfil = pastaUsuario($atletaId) . '/perfil.json';
    gravarJson($perfil, array_merge(lerJson($perfil) ?? ['criado_em' => $agora], ['ultimo_login' => $agora]));

    return $token;
}

function atletaDaSessao(): int
{
    $token = tokenDoCabecalho() ?? throw new ErroHttp(401, 'Sessão ausente. Conecte o Strava novamente.');
    $sessao = lerJson(caminhoDados('sessoes', hash('sha256', $token) . '.json'))
        ?? throw new ErroHttp(401, 'Sessão inválida. Conecte o Strava novamente.');
    return (int) $sessao['atleta_id'];
}

function encerrarSessao(): void
{
    $token = tokenDoCabecalho();
    if ($token === null) {
        return;
    }
    $hash = hash('sha256', $token);
    $arquivo = caminhoDados('sessoes', "$hash.json");
    $sessao = lerJson($arquivo);
    if ($sessao !== null) {
        @unlink(pastaUsuario((int) $sessao['atleta_id']) . "/sessoes/$hash");
    }
    @unlink($arquivo);
}

/** Exclusão de conta (LGPD / exigência da Play Store): apaga sessões, perfil e backups. */
function excluirUsuario(int $atletaId): void
{
    foreach (glob(pastaUsuario($atletaId) . '/sessoes/*') ?: [] as $marcador) {
        @unlink(caminhoDados('sessoes', basename($marcador) . '.json'));
    }
    removerPasta(pastaUsuario($atletaId));
}
