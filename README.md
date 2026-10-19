# ToolBox
Central de ferramentas digitais em português. Java 21, Spring Boot 3.5, PDFBox e ZXing; interface responsiva em HTML/CSS/JavaScript.

## Executar
Requisitos: Java 21, Python 3.12+ (bootstrap), curl e acesso ao Maven Central. Não requer banco, login ou chave de API.

```sh
./scripts/maven.sh verify
./scripts/maven.sh spring-boot:run
```

O servidor usa a porta 8080 (configurável por `PORT`). Saúde: `GET /api/health`. O script instala Maven 3.9.9 com verificação SHA-512 e configura o proxy HTTPS fornecido pelo ambiente. Cache padrão: `/workspace/tools`, alterável por `TOOLBOX_TOOLS_DIR`.

## Disponível
- Juntar de 2 a 20 PDFs e extrair uma página de PDF.
- Converter PNG/JPG e redimensionar preservando proporção.
- Gerar QR Code de texto, link, Wi-Fi WPA/WPA2 e WhatsApp.
- Compactar até 50 arquivos em ZIP com nomes normalizados.
- Contador de palavras, formatação/validação JSON, senhas criptograficamente aleatórias e SHA-256 no navegador.
- Busca, filtros por categoria, favoritos locais e sugestão de categoria pelo formato do arquivo.

As operações do servidor retornam downloads. Limites: 20 MB por arquivo, 50 MB por requisição e 25 megapixels por imagem. Uploads temporários são gerenciados pelo Spring; resultados não são persistidos. As ferramentas locais de texto/hash não enviam conteúdo ao servidor. Fontes Google são opcionais; existem fontes de fallback.

## API
POST multipart `/api/pdf/merge` (`files`), `/api/pdf/split` (`file`, `page`), `/api/image/convert` (`file`, `format=png|jpg`, `width` opcional), `/api/qr` (`text`), `/api/zip` (`files`). Erros retornam status 400 e JSON com `message`.

## Escopo e próximas etapas
Esta é a primeira versão funcional, não a implementação completa da proposta. Word/PDF exige LibreOffice e integração isolada; remoção de fundo exige modelo ONNX confiável e recursos de inferência. WebP, OCR, conversão de planilhas, leitura de QR e conversão PDF/Word ainda não estão implementados. Contas, histórico e PostgreSQL ficam para uma etapa posterior. A conversão inteligente atual sugere categorias, não envia arquivos automaticamente.

Antes de exposição pública, adicionar quotas/rate limiting e isolamento de processamento com limites de tempo e memória para documentos não confiáveis. O processamento atual é síncrono; PDFs grandes e concorrência elevada não foram validados.

## Validação
`./scripts/maven.sh verify` executa testes de integração: união/extração de PDFs, round-trip de QR, dimensões de imagens, ZIP seguro e entradas inválidas. Não há dependência de serviços externos no runtime.
