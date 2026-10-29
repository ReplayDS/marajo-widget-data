# Marajó Widget para Android

Widget nativo Android da unidade 3001 — Porangatu 01.

## Instalação
Abra a aba Actions do repositório, selecione "Compilar APK Marajó Android", baixe o artefato `marajo-widget-android` e extraia o APK do ZIP. Instale no Android (permitir instalação desta fonte, se solicitado). Depois pressione a tela inicial, selecione Widgets e adicione "Marajó Widget".

## Dados
Somente o JSON público `https://raw.githubusercontent.com/ReplayDS/marajo-widget-data/main/dados.json`. Nenhum token é incorporado no APK.

## Atualização
Solicita atualização periódica por AlarmManager, além de atualizações do sistema e toque manual. O Android pode atrasar tarefas em segundo plano. O volume exibido é limitado ao dia corrente em America/Sao_Paulo.

## Build local
Com JDK 17, Android SDK 35 e Gradle 8.9: `gradle :app:assembleDebug` a partir da pasta `android`.
