<?php

declare(strict_types=1);

function caminhoDados(string ...$partes): string
{
    return rtrim((string) config('dir_dados'), '/') . '/' . implode('/', $partes);
}

function garantirPasta(string $pasta): string
{
    if (!is_dir($pasta) && !mkdir($pasta, 0700, true) && !is_dir($pasta)) {
        throw new RuntimeException("Não foi possível criar $pasta");
    }
    return $pasta;
}

/** Grava via arquivo temporário + rename para nunca deixar um JSON pela metade. */
function gravarJson(string $arquivo, array $dados): void
{
    garantirPasta(dirname($arquivo));
    $temporario = $arquivo . '.' . bin2hex(random_bytes(4)) . '.tmp';
    file_put_contents($temporario, json_encode($dados, JSON_UNESCAPED_UNICODE | JSON_PRETTY_PRINT), LOCK_EX);
    rename($temporario, $arquivo);
}

function lerJson(string $arquivo): ?array
{
    if (!is_file($arquivo)) {
        return null;
    }
    $dados = json_decode((string) file_get_contents($arquivo), true);
    return is_array($dados) ? $dados : null;
}

function removerPasta(string $pasta): void
{
    if (!is_dir($pasta)) {
        return;
    }
    $itens = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($pasta, FilesystemIterator::SKIP_DOTS),
        RecursiveIteratorIterator::CHILD_FIRST,
    );
    foreach ($itens as $item) {
        $item->isDir() ? rmdir($item->getPathname()) : unlink($item->getPathname());
    }
    rmdir($pasta);
}
