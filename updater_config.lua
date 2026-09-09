-- ============================================================================
-- SCRIPT DE CONFIGURACIÓN Y VERIFICACIÓN DE RELEASES / ACTUALIZACIONES (LUA 5.4)
-- Repositorio oficial: LuisAlejandro544/Keeppr
-- ============================================================================
-- Este script es ejecutado de manera nativa por el motor C++/Lua de Keeppr
-- para desacoplar las URLs de releases y la lógica de filtrado de versiones beta.
-- ============================================================================

local UpdaterConfig = {}

-- 1. IDENTIFICACIÓN DEL REPOSITORIO Y CANAL
UpdaterConfig.repo_owner = "LuisAlejandro544"
UpdaterConfig.repo_name = "Keeppr"
UpdaterConfig.target_channel = "beta"
UpdaterConfig.beta_tag_suffix = "-b"

-- 2. ENDPOINTS Y ENLACES OFICIALES
UpdaterConfig.endpoints = {
    -- API de GitHub para consultar todos los releases y pre-releases del repositorio
    releases_api = "https://api.github.com/repos/LuisAlejandro544/Keeppr/releases",
    
    -- Página oficial de releases en GitHub
    releases_web = "https://github.com/LuisAlejandro544/Keeppr/releases",
    
    -- Script de configuración remoto en GitHub Raw para actualización al vuelo
    config_raw = "https://raw.githubusercontent.com/LuisAlejandro544/Keeppr/main/updater_config.lua",
    
    -- Registro de cambios oficial de la versión Beta en formato texto plano
    changelog_raw = "https://raw.githubusercontent.com/LuisAlejandro544/Keeppr/main/Changelog-beta.md"
}

-- 3. FILTRADO DE RELEASES BETA
--- Evalúa si un release recibido de la API de GitHub corresponde a una versión Beta válida.
-- En Keeppr, las versiones Beta deben ser obligatoriamente Pre-Releases y poseer el sufijo '-b' en su tag.
-- @param tag_name (string) Ejemplo: "v0.1.0-b"
-- @param is_prerelease (boolean) Si la release de GitHub está marcada como pre-release
-- @return (boolean) true si es un release beta elegible, false en caso contrario
function UpdaterConfig.filter_beta_release(tag_name, is_prerelease)
    if not is_prerelease then
        return false
    end
    if not tag_name or type(tag_name) ~= "string" then
        return false
    end
    -- Debe contener '-b' (o terminar en '-b')
    if string.find(tag_name, "%-b") then
        return true
    end
    return false
end

-- 4. COMPARACIÓN DE VERSIONES
--- Compara la versión actualmente instalada en el dispositivo con la versión remota.
-- @param current_tag (string) Ejemplo: "0.1.0-b" o "v0.1.0-b"
-- @param remote_tag (string) Ejemplo: "v0.1.1-b"
-- @return (boolean) true si remote_tag es más reciente o diferente, false si ya está al día
function UpdaterConfig.is_newer_version(current_tag, remote_tag)
    if not remote_tag or remote_tag == "" then
        return false
    end
    if not current_tag or current_tag == "" then
        return true
    end

    -- Normalizar removiendo 'v' inicial y espacios
    local c = string.gsub(string.gsub(current_tag, "^v", ""), "%s+", "")
    local r = string.gsub(string.gsub(remote_tag, "^v", ""), "%s+", "")

    if c == r then
        return false
    end

    -- Extraer componentes numéricos principales mayores/menores/parches
    local c_maj, c_min, c_patch = string.match(c, "(%d+)%.(%d+)%.(%d+)")
    local r_maj, r_min, r_patch = string.match(r, "(%d+)%.(%d+)%.(%d+)")

    if c_maj and r_maj then
        local c1, c2, c3 = tonumber(c_maj) or 0, tonumber(c_min) or 0, tonumber(c_patch) or 0
        local r1, r2, r3 = tonumber(r_maj) or 0, tonumber(r_min) or 0, tonumber(r_patch) or 0

        if r1 > c1 then return true end
        if r1 < c1 then return false end
        if r2 > c2 then return true end
        if r2 < c2 then return false end
        if r3 > c3 then return true end
        if r3 < c3 then return false end
    end

    -- Si difieren en sufijo u otro campo y no son idénticos
    return c ~= r
end

-- 5. SERIALIZACIÓN JSON PARA INTEROPERABILIDAD CON KOTLIN
--- Genera una representación JSON limpia consumible directamente por Android sin dependencias externas.
function UpdaterConfig.get_config_json()
    local json = "{"
    json = json .. "\"repo_owner\":\"" .. UpdaterConfig.repo_owner .. "\","
    json = json .. "\"repo_name\":\"" .. UpdaterConfig.repo_name .. "\","
    json = json .. "\"target_channel\":\"" .. UpdaterConfig.target_channel .. "\","
    json = json .. "\"beta_tag_suffix\":\"" .. UpdaterConfig.beta_tag_suffix .. "\","
    json = json .. "\"releases_api\":\"" .. UpdaterConfig.endpoints.releases_api .. "\","
    json = json .. "\"releases_web\":\"" .. UpdaterConfig.endpoints.releases_web .. "\","
    json = json .. "\"config_raw\":\"" .. UpdaterConfig.endpoints.config_raw .. "\","
    json = json .. "\"changelog_raw\":\"" .. UpdaterConfig.endpoints.changelog_raw .. "\""
    json = json .. "}"
    return json
end

return UpdaterConfig
