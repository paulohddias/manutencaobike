<?php

declare(strict_types=1);

function atenderRequisicao(): never
{
    $metodo = $_SERVER['REQUEST_METHOD'] ?? 'GET';
    $rota = trim((string) ($_GET['rota'] ?? ''), '/');

    try {
        match (true) {
            $metodo === 'GET' && $rota === 'status' => responderJson(200, ['ok' => true, 'versao' => 1]),

            $metodo === 'POST' && $rota === 'strava/token' => (function (): never {
                $codigo = lerCorpoJson()['code'] ?? null;
                if (!is_string($codigo) || !preg_match('/^[A-Za-z0-9]{10,100}$/', $codigo)) {
                    throw new ErroHttp(400, 'Código de autorização inválido.');
                }
                $dados = stravaOauthToken(['code' => $codigo, 'grant_type' => 'authorization_code']);
                $atletaId = (int) ($dados['athlete']['id'] ?? 0);
                if ($atletaId <= 0) {
                    throw new ErroHttp(502, 'O Strava não informou o atleta.');
                }
                responderJson(200, ['sessao' => criarSessao($atletaId), 'token' => tokenParaApp($dados)]);
            })(),

            $metodo === 'POST' && $rota === 'strava/refresh' => (function (): never {
                atletaDaSessao();
                $refresh = lerCorpoJson()['refresh_token'] ?? null;
                if (!is_string($refresh) || !preg_match('/^[A-Za-z0-9]{10,200}$/', $refresh)) {
                    throw new ErroHttp(400, 'Refresh token inválido.');
                }
                responderJson(200, tokenParaApp(stravaOauthToken(['refresh_token' => $refresh, 'grant_type' => 'refresh_token'])));
            })(),

            $metodo === 'DELETE' && $rota === 'sessao' => (function (): never {
                encerrarSessao();
                responderJson(200, ['ok' => true]);
            })(),

            $metodo === 'GET' && $rota === 'backups' => responderJson(200, ['backups' => listarBackups(atletaDaSessao())]),

            $metodo === 'POST' && $rota === 'backups' => responderJson(201, receberBackup(atletaDaSessao())),

            $metodo === 'GET' && str_starts_with($rota, 'backups/') => enviarBackup(atletaDaSessao(), substr($rota, 8)),

            $metodo === 'DELETE' && $rota === 'conta' => (function (): never {
                excluirUsuario(atletaDaSessao());
                responderJson(200, ['ok' => true]);
            })(),

            default => throw new ErroHttp(404, 'Rota não encontrada.'),
        };
    } catch (ErroHttp $e) {
        responderJson($e->status, ['erro' => $e->getMessage()]);
    } catch (Throwable $e) {
        error_log('[manutencaobike] ' . $e);
        responderJson(500, ['erro' => 'Erro interno no servidor.']);
    }
}
