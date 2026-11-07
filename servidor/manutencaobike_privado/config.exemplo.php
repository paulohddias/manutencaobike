<?php

// Copie para config.php (mesma pasta) e preencha. O config.php nunca vai para o Git.
return [
    // https://www.strava.com/settings/api
    'strava_client_id' => '294700',
    'strava_client_secret' => 'COLE_AQUI_O_SEGREDO_DO_CLIENTE',

    // Fora do public_html: só o PHP lê e grava aqui.
    'dir_dados' => __DIR__ . '/dados',

    'backup_max_bytes' => 50 * 1024 * 1024,
    'backups_por_usuario' => 5,
    'intervalo_minimo_segundos' => 60,
];
