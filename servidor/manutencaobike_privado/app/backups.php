<?php

declare(strict_types=1);

const PADRAO_ID_BACKUP = '/^\d{8}-\d{6}-[a-f0-9]{6}$/';

function pastaBackups(int $atletaId): string
{
    return pastaUsuario($atletaId) . '/backups';
}

/** Mais recentes primeiro. */
function listarBackups(int $atletaId): array
{
    $itens = [];
    foreach (glob(pastaBackups($atletaId) . '/*.zip') ?: [] as $arquivo) {
        $id = basename($arquivo, '.zip');
        if (preg_match(PADRAO_ID_BACKUP, $id)) {
            $itens[] = ['id' => $id, 'criado_em' => filemtime($arquivo), 'tamanho' => filesize($arquivo)];
        }
    }
    usort($itens, fn(array $a, array $b) => strcmp($b['id'], $a['id']));
    return $itens;
}

/** Recebe o .zip enviado no corpo da requisição (Content-Type: application/zip). */
function receberBackup(int $atletaId): array
{
    $limite = (int) config('backup_max_bytes');
    if ((int) ($_SERVER['CONTENT_LENGTH'] ?? 0) > $limite) {
        throw new ErroHttp(413, 'Backup maior que o permitido.');
    }
    $existentes = listarBackups($atletaId);
    if ($existentes !== [] && time() - $existentes[0]['criado_em'] < (int) config('intervalo_minimo_segundos')) {
        throw new ErroHttp(429, 'Aguarde um pouco antes de enviar outro backup.');
    }

    $pasta = garantirPasta(pastaBackups($atletaId));
    $temporario = $pasta . '/envio-' . bin2hex(random_bytes(6)) . '.tmp';
    try {
        $total = copiarCorpoComLimite($temporario, $limite);
        if (!zipDeBackupValido($temporario)) {
            throw new ErroHttp(422, 'Arquivo de backup inválido.');
        }
        $id = gmdate('Ymd-His') . '-' . bin2hex(random_bytes(3));
        rename($temporario, "$pasta/$id.zip");
    } finally {
        @unlink($temporario);
    }

    foreach (array_slice(listarBackups($atletaId), (int) config('backups_por_usuario')) as $antigo) {
        @unlink("$pasta/{$antigo['id']}.zip");
    }
    return ['id' => $id, 'criado_em' => time(), 'tamanho' => $total];
}

/** Copia em blocos: o Content-Length pode mentir, então o limite é conferido durante a leitura. */
function copiarCorpoComLimite(string $destino, int $limite): int
{
    $entrada = fopen('php://input', 'rb');
    $saida = fopen($destino, 'wb');
    $total = 0;
    try {
        while (!feof($entrada)) {
            $bloco = fread($entrada, 65536);
            if ($bloco === false) {
                throw new ErroHttp(400, 'Falha ao receber o arquivo.');
            }
            $total += strlen($bloco);
            if ($total > $limite) {
                throw new ErroHttp(413, 'Backup maior que o permitido.');
            }
            fwrite($saida, $bloco);
        }
    } finally {
        fclose($entrada);
        fclose($saida);
    }
    if ($total === 0) {
        throw new ErroHttp(400, 'Nenhum arquivo enviado.');
    }
    return $total;
}

function zipDeBackupValido(string $arquivo): bool
{
    $assinatura = file_get_contents($arquivo, length: 4);
    if ($assinatura !== "PK\x03\x04") {
        return false;
    }
    if (!class_exists(ZipArchive::class)) {
        return true;
    }
    $zip = new ZipArchive();
    if ($zip->open($arquivo, ZipArchive::RDONLY) !== true) {
        return false;
    }
    $valido = $zip->locateName('manifesto.json') !== false && $zip->locateName('banco.db') !== false;
    $zip->close();
    return $valido;
}

function enviarBackup(int $atletaId, string $id): never
{
    if (!preg_match(PADRAO_ID_BACKUP, $id)) {
        throw new ErroHttp(404, 'Backup não encontrado.');
    }
    $arquivo = pastaBackups($atletaId) . "/$id.zip";
    if (!is_file($arquivo)) {
        throw new ErroHttp(404, 'Backup não encontrado.');
    }
    header('Content-Type: application/zip');
    header('Content-Length: ' . filesize($arquivo));
    header('Cache-Control: no-store');
    readfile($arquivo);
    exit;
}
