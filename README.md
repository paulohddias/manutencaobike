# Manutenção Bike

App Android para controlar a manutenção de bicicletas: desgaste de peças pela quilometragem, serviços periódicos (revisões) e custos, com as bikes e o km importados do Strava.

## Funcionalidades

- **Bikes do Strava:** importa as bikes da conta e atualiza o km automaticamente.
- **Bikes manuais:** cadastro sem Strava, com km informado à mão.
- **Peças:** cada peça instalada tem uma vida útil em km. O app avisa ao chegar em 80% e pede a troca em 100%. Na troca, a peça antiga vai para o histórico com os km que durou de verdade.
- **Serviços:** registra revisões e outros serviços com data, km e custo. Os tipos com intervalo (ex.: revisão a cada 2.000 km ou 6 meses) geram lembretes.
- **Catálogo editável:** tipos de peças e serviços que já vêm prontos e podem ser alterados.
- **Notificações:** uma vez por dia o app sincroniza com o Strava e avisa sobre peças e serviços vencidos.
- **Custos:** gasto total por bike, separado em peças e serviços.

## Configurar o Strava

1. Acesse <https://www.strava.com/settings/api> e crie um aplicativo.
2. Em **Authorization Callback Domain**, coloque `localhost`.
3. Copie o **Client ID** e o **Client Secret** para o arquivo `local.properties` na raiz do projeto, que não vai para o Git:

   ```properties
   strava.clientId=12345
   strava.clientSecret=seu_client_secret
   ```

4. Recompile o app e toque em **Conectar com o Strava**.

> O client secret fica embutido no APK, o que é aceitável para uso pessoal. Antes de publicar na Play Store, mova a troca de tokens para um servidor.

## Tecnologias

Kotlin, Jetpack Compose (Material 3), Hilt, Room, Navigation Compose, Retrofit + kotlinx.serialization, DataStore e WorkManager.
