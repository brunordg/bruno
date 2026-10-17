package writer

import com.codeteam.model.ValidationConstraints
import com.codeteam.writer.ExampleValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleValuesTest {

    private fun value(name: String, type: String = "String", constraints: ValidationConstraints = ValidationConstraints()) =
        ExampleValues.valueFor(name, type, constraints)

    // --- 2.1 tokenize ---

    @Test
    fun `tokenize splits camelCase, snake_case, acronyms and digits and strips accents`() {
        assertEquals(listOf("first", "name"), ExampleValues.tokenize("firstName"))
        assertEquals(listOf("nome", "completo"), ExampleValues.tokenize("nome_completo"))
        assertEquals(listOf("descricao"), ExampleValues.tokenize("descrição"))
        assertEquals(listOf("user", "id"), ExampleValues.tokenize("userID"))
        assertEquals(listOf("url", "path"), ExampleValues.tokenize("URLPath"))
        assertEquals(listOf("address", "2", "line"), ExampleValues.tokenize("address2-line"))
    }

    // --- 2.2 id, dictionary, type, fallback ---

    @Test
    fun `id is the whole name or the last word`() {
        assertEquals("{{\$randomInt}}", value("id", "Long"))
        assertEquals("{{\$randomUUID}}", value("userId", "String"))
        assertEquals("{{\$randomUUID}}", value("user_id", "UUID"))
    }

    @Test
    fun `substrings inside a word do not match`() {
        assertEquals("{{\$randomBoolean}}", value("paid", "boolean"))
        assertEquals("{{\$randomLoremWord}}", value("cancelReason"))
        assertEquals("{{\$randomLoremWord}}", value("updatedBy"))
        assertEquals("{{\$randomInt}}", value("timeout", "int"))
        assertEquals("{{\$randomLoremWord}}", value("paisagem"))
        assertEquals("{{\$randomLoremWord}}", value("crua"))
    }

    @Test
    fun `dictionary maps English and Portuguese words to the same variable`() {
        val cases = mapOf(
            listOf("firstName", "primeiroNome") to "{{\$randomFirstName}}",
            listOf("lastName", "sobrenome", "ultimoNome") to "{{\$randomLastName}}",
            listOf("name", "fullName", "nome", "nomeCompleto") to "{{\$randomFullName}}",
            listOf("username", "userName", "login", "usuario", "nomeUsuario") to "{{\$randomUserName}}",
            listOf("email", "contactEmail", "mail") to "{{\$randomEmail}}",
            listOf("password", "pwd", "senha") to "{{\$randomPassword}}",
            listOf("phone", "mobile", "telefone", "celular", "fone") to "{{\$randomPhoneNumber}}",
            listOf("street", "address", "rua", "logradouro", "endereco") to "{{\$randomStreetAddress}}",
            listOf("city", "cidade", "municipio") to "{{\$randomCity}}",
            listOf("countryCode", "codigoPais") to "{{\$randomCountryCode}}",
            listOf("country", "pais") to "{{\$randomCountry}}",
            listOf("company", "companyName", "empresa", "razaoSocial") to "{{\$randomCompanyName}}",
            listOf("jobTitle", "job", "occupation", "cargo", "profissao") to "{{\$randomJobTitle}}",
            listOf("url", "website", "site", "link") to "{{\$randomUrl}}",
            listOf("avatar", "photo", "image", "foto", "imagem") to "{{\$randomAvatarImage}}",
            listOf("ip") to "{{\$randomIP}}",
            listOf("latitude", "lat") to "{{\$randomLatitude}}",
            listOf("longitude", "lng", "lon") to "{{\$randomLongitude}}",
            listOf("currency", "moeda") to "{{\$randomCurrencyCode}}",
            listOf("color", "cor") to "{{\$randomHexColor}}",
            listOf("fileName", "nomeArquivo") to "{{\$randomFileName}}",
            listOf("mimeType", "contentType") to "{{\$randomMimeType}}",
            listOf("locale", "language", "idioma") to "{{\$randomLocale}}",
            listOf("slug") to "{{\$randomLoremSlug}}",
            listOf("version", "versao") to "{{\$randomSemver}}",
            listOf("description", "comment", "notes", "descricao", "descrição", "observacao", "comentario") to
                "{{\$randomLoremSentence}}",
            listOf("zip", "postalCode", "cep") to "{{\$randomInt}}"
        )
        for ((names, expected) in cases) {
            for (name in names) assertEquals("field '$name'", expected, value(name))
        }
    }

    @Test
    fun `price words depend on the numeric type`() {
        assertEquals("{{\$randomPrice}}", value("price", "BigDecimal"))
        assertEquals("{{\$randomPrice}}", value("valor", "double"))
        assertEquals("{{\$randomInt}}", value("total", "int"))
        assertEquals("{{\$randomInt}}", value("preco", "Long"))
    }

    @Test
    fun `the rule matching more words wins`() {
        assertEquals("{{\$randomCountryCode}}", value("countryCode"))
        assertEquals("{{\$randomCountry}}", value("country"))
        assertEquals("{{\$randomFileName}}", value("fileName"))
    }

    @Test
    fun `boolean and UUID are identified by type`() {
        assertEquals("{{\$randomBoolean}}", value("ativo", "boolean"))
        assertEquals("{{\$randomBoolean}}", value("enabled", "Boolean"))
        assertEquals("{{\$randomUUID}}", value("token", "UUID"))
        assertEquals("{{\$randomUUID}}", value("token", "java.util.UUID"))
    }

    @Test
    fun `unrecognized fields keep the current fallback`() {
        assertEquals("{{\$randomLoremWord}}", value("misc"))
        assertEquals("{{\$randomInt}}", value("misc", "int"))
        assertEquals("{{\$randomPrice}}", value("misc", "BigDecimal"))
        assertEquals("{{payload}}", value("payload", "Duration"))
        assertEquals("{{point}}", value("point", "Point"))
    }

    // --- 2.3 dates ---

    @Test
    fun `date types map to the script variables or isoTimestamp`() {
        assertEquals("{{randomDate}}", value("eventDay", "LocalDate"))
        assertEquals("{{randomDateTime}}", value("scheduledAt", "LocalDateTime"))
        assertEquals("{{randomTime}}", value("opensAt", "LocalTime"))
        assertEquals("{{\$isoTimestamp}}", value("createdAt", "Instant"))
        assertEquals("{{\$isoTimestamp}}", value("when", "OffsetDateTime"))
        assertEquals("{{\$isoTimestamp}}", value("when", "ZonedDateTime"))
        assertEquals("{{\$isoTimestamp}}", value("when", "Date"))
    }

    @Test
    fun `LocalDate uses past or future variant from words`() {
        assertEquals("{{randomPastDate}}", value("dataNascimento", "LocalDate"))
        assertEquals("{{randomPastDate}}", value("birthDate", "LocalDate"))
        assertEquals("{{randomPastDate}}", value("criadoEm", "LocalDate"))
        assertEquals("{{randomFutureDate}}", value("dueDate", "LocalDate"))
        assertEquals("{{randomFutureDate}}", value("dataVencimento", "LocalDate"))
    }

    @Test
    fun `LocalDate constraints win over name words`() {
        assertEquals("{{randomFutureDate}}", value("startsOn", "LocalDate", ValidationConstraints(future = true)))
        assertEquals("{{randomPastDate}}", value("dueDate", "LocalDate", ValidationConstraints(past = true)))
        assertEquals("{{randomFutureDate}}", value("birthDate", "LocalDate", ValidationConstraints(future = true)))
    }

    @Test
    fun `start and end words pick the shared-day variables for every date type`() {
        assertEquals("{{randomStartDateTime}}", value("startDateTime", "LocalDateTime"))
        assertEquals("{{randomEndDateTime}}", value("endDateTime", "LocalDateTime"))
        assertEquals("{{randomStartDate}}", value("dataInicio", "LocalDate"))
        assertEquals("{{randomEndDate}}", value("dataFim", "LocalDate"))
        assertEquals("{{randomStartTime}}", value("horaInicio", "LocalTime"))
        assertEquals("{{randomEndTime}}", value("until", "LocalTime"))
        assertEquals("{{randomStartTimestamp}}", value("startsAt", "Instant"))
        assertEquals("{{randomEndTimestamp}}", value("endsAt", "OffsetDateTime"))
        assertEquals("{{randomStartTimestamp}}", value("beginAt", "ZonedDateTime"))
        assertEquals("{{randomEndTimestamp}}", value("dataTermino", "Date"))
    }

    @Test
    fun `start words only apply to date types and constraints win`() {
        assertEquals("{{\$randomLoremWord}}", value("fromAccount", "String"))
        assertEquals("{{randomFutureDate}}", value("startDate", "LocalDate", ValidationConstraints(future = true)))
        assertEquals("{{randomStartDate}}", value("startBirthday", "LocalDate"))
    }

    @Test
    fun `date script sets all variables`() {
        val script = ExampleValues.DATE_SCRIPT
        assertTrue(script.startsWith("script:pre-request {"))
        val names = listOf("randomDate", "randomPastDate", "randomFutureDate", "randomDateTime", "randomTime") +
            listOf("Date", "DateTime", "Time", "Timestamp").flatMap { listOf("randomStart$it", "randomEnd$it") }
        for (name in names) {
            assertTrue(name, script.contains("bru.setVar(\"$name\""))
        }
    }

    // --- 2.4 pagination and constraints ---

    @Test
    fun `pagination parameters get fixed values`() {
        assertEquals("0", value("page", "int"))
        assertEquals("0", value("pagina", "Integer"))
        assertEquals("20", value("size", "int"))
        assertEquals("20", value("limit", "int"))
        assertEquals("20", value("pageSize", "int"))
        assertEquals("20", value("tamanho", "int"))
        assertEquals("20", value("limite", "Long"))
        assertEquals("{{\$randomLoremWord}}", value("page", "String"))
    }

    @Test
    fun `Positive without Min gives at least one`() {
        assertEquals("1", value("quantidade", "int", ValidationConstraints(positive = true)))
        assertEquals("{{\$randomInt}}", value("quantidade", "int"))
    }

    @Test
    fun `small Size max turns a sentence into a word`() {
        assertEquals("{{\$randomLoremWord}}", value("description", "String", ValidationConstraints(maxSize = 10)))
        assertEquals("{{\$randomLoremSentence}}", value("description", "String", ValidationConstraints(maxSize = 255)))
    }
}
