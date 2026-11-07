# Servidor do Manutenção Bike (PHP 8.3)

API usada pelo app para:

- trocar e renovar o token do Strava (o `client_secret` fica só aqui, nunca no app);
- guardar os backups dos usuários (até 5 por usuário, 50 MB cada);
- excluir os dados do usuário (exigência da LGPD e da Play Store).

O usuário é identificado pelo id de atleta do Strava. Ao conectar, o servidor gera um token de
sessão aleatório; em disco fica só o hash SHA-256 dele.

## Estrutura

```
/home/USUARIO_CPANEL/
├── manutencaobike_privado/          ← FORA do public_html (não acessível pela web)
│   ├── bootstrap.php
│   ├── config.php                   ← criado a partir do config.exemplo.php
│   ├── app/*.php
│   └── dados/                       ← criado sozinho: sessões, perfis e backups
└── api.viavalesistemas.com.br/      ← raiz do subdomínio
    └── manutencaobike/v1/
        ├── index.php
        ├── .htaccess
        └── .user.ini
```

O `index.php` procura a pasta `manutencaobike_privado` subindo a partir dele, então funciona tanto
com a raiz do subdomínio em `/home/USUARIO/api.viavalesistemas.com.br` quanto em
`/home/USUARIO/public_html/api`.

## Instalação no HostGator (cPanel)

1. **Subdomínio:** cPanel → *Domínios* (ou *Subdomínios*) → criar `api.viavalesistemas.com.br`.
   Anote a "Raiz do documento" que o cPanel sugerir.
2. **SSL:** cPanel → *SSL/TLS Status* → *Run AutoSSL* e confirme que `api.viavalesistemas.com.br`
   ficou com cadeado verde. O app só fala com HTTPS.
3. **PHP 8.3:** cPanel → *MultiPHP Manager* → selecione o subdomínio → PHP 8.3.
   Em *Select PHP Version* / *Extensions*, confirme que `curl` e `zip` estão marcadas.
4. **Arquivos:** pelo *Gerenciador de Arquivos* (ou FTP):
   - envie a pasta `manutencaobike_privado/` para `/home/USUARIO/` (ao lado do `public_html`);
   - envie a pasta `manutencaobike/` para a raiz do subdomínio.
   Arquivos que começam com ponto (`.htaccess`, `.user.ini`) ficam ocultos: ative
   *Configurações → Mostrar arquivos ocultos* no Gerenciador de Arquivos.
5. **Configuração:** dentro de `manutencaobike_privado/`, copie `config.exemplo.php` para
   `config.php` e preencha `strava_client_secret` (Strava → Configurações → API →
   "Segredo do cliente"). Permissão do arquivo: `600`.
6. **Teste:** abra `https://api.viavalesistemas.com.br/manutencaobike/v1/status` no navegador.
   Deve aparecer `{"ok":true,"versao":1}`.

## Endpoints

| Método | Rota | Autenticação | Descrição |
|---|---|---|---|
| GET | `status` | — | Verificação de funcionamento |
| POST | `strava/token` | — | `{"code": "..."}` → `{"sessao": "...", "token": {...}}` |
| POST | `strava/refresh` | sessão | `{"refresh_token": "..."}` → token novo |
| DELETE | `sessao` | sessão | Encerra a sessão (desconectar) |
| GET | `backups` | sessão | Lista os backups do usuário |
| POST | `backups` | sessão | Corpo = `.zip` (`Content-Type: application/zip`) |
| GET | `backups/{id}` | sessão | Download do `.zip` |
| DELETE | `conta` | sessão | Apaga backups, sessões e perfil do usuário |

Sessão = cabeçalho `Authorization: Bearer <token>`.

## Teste local

Com PHP 8.3 na máquina (ex.: Laragon), monte a mesma estrutura numa pasta qualquer e rode o
servidor embutido com um roteador que preencha `$_GET['rota']`:

```php
<?php // router.php
$_GET['rota'] = substr(parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH), strlen('/manutencaobike/v1/'));
require __DIR__ . '/pub/manutencaobike/v1/index.php';
```

```
php -S 127.0.0.1:8765 router.php
adb reverse tcp:8765 tcp:8765
```

e no `local.properties` do app: `api.url=http://127.0.0.1:8765/manutencaobike/v1/`
(o build de debug permite HTTP só para 127.0.0.1/localhost).
