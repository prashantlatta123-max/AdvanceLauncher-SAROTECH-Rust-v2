use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Deserialize)]
pub struct AppMetaInput {
    pub package_name: String,
    pub label: String,
    #[serde(default)]
    pub launch_count: i32,
}

#[derive(Debug, Clone, Serialize)]
pub struct CategorizedApp {
    pub package_name: String,
    pub category: String,
}

pub fn classify_app(pkg: &str, label: &str) -> &'static str {
    let p = pkg.to_lowercase();
    let l = label.to_lowercase();

    if p.contains("whatsapp") || p.contains("instagram") || p.contains("facebook")
        || p.contains("telegram") || p.contains("snapchat") || p.contains("twitter")
        || p.contains("discord") || p.contains("reddit") || p.contains("linkedin")
        || p.contains("messaging") || p.contains("dialer") || p.contains("contacts")
        || l.contains("chat") || l.contains("messages") || l.contains("phone")
    {
        return "Social";
    }
    if p.contains("youtube") || p.contains("spotify") || p.contains("netflix")
        || p.contains("music") || p.contains("video") || p.contains("gallery")
        || p.contains("photos") || p.contains("camera") || p.contains("mxtech")
        || p.contains("vlc") || p.contains("hotstar") || p.contains("jio")
        || l.contains("music") || l.contains("camera") || l.contains("photos")
    {
        return "Media";
    }
    if p.contains("pubg") || p.contains("bgmi") || p.contains("game")
        || p.contains("freefire") || p.contains("supercell") || p.contains("roblox")
        || p.contains("minecraft") || p.contains("chess") || p.contains("ludo")
        || l.contains("game")
    {
        return "Games";
    }
    if p.contains("pay") || p.contains("bank") || p.contains("upi")
        || p.contains("phonepe") || p.contains("paytm") || p.contains("groww")
        || p.contains("zerodha") || p.contains("coin") || p.contains("sbi")
        || p.contains("hdfc") || p.contains("icici") || l.contains("pay") || l.contains("bank")
    {
        return "Finance";
    }
    if p.contains("amazon") || p.contains("flipkart") || p.contains("myntra")
        || p.contains("meesho") || p.contains("swiggy") || p.contains("zomato")
        || p.contains("blinkit") || p.contains("zepto") || p.contains("shop")
    {
        return "Shopping";
    }
    if p.contains("docs") || p.contains("sheets") || p.contains("slides")
        || p.contains("drive") || p.contains("office") || p.contains("mail")
        || p.contains("gmail") || p.contains("calendar") || p.contains("notes")
        || p.contains("keep") || p.contains("notion") || p.contains("pdf")
        || p.contains("reader") || p.contains("github") || l.contains("notes") || l.contains("calendar")
    {
        return "Productivity";
    }
    if p.contains("settings") || p.contains("android.system") || p.contains("launcher")
        || p.contains("vending") || p.contains("playstore") || l.contains("settings") || l.contains("play store")
    {
        return "System";
    }
    if p.contains("calc") || p.contains("clock") || p.contains("file")
        || p.contains("browser") || p.contains("chrome") || p.contains("firefox")
        || p.contains("maps") || p.contains("weather") || p.contains("compass")
        || p.contains("recorder") || l.contains("files") || l.contains("clock") || l.contains("calculator")
    {
        return "Tools";
    }
    "Other"
}

pub fn sort_and_categorize_json(apps_json: &str, sort_mode: &str) -> String {
    let mut apps: Vec<AppMetaInput> = match serde_json::from_str(apps_json) {
        Ok(v) => v,
        Err(_) => return "[]".to_string(),
    };

    match sort_mode {
        "ZA" => apps.sort_by(|a, b| b.label.to_lowercase().cmp(&a.label.to_lowercase())),
        "MostUsed" => apps.sort_by(|a, b| {
            b.launch_count
                .cmp(&a.launch_count)
                .then_with(|| a.label.to_lowercase().cmp(&b.label.to_lowercase()))
        }),
        _ => apps.sort_by(|a, b| a.label.to_lowercase().cmp(&b.label.to_lowercase())),
    }

    let out: Vec<CategorizedApp> = apps
        .into_iter()
        .map(|app| CategorizedApp {
            category: classify_app(&app.package_name, &app.label).to_string(),
            package_name: app.package_name,
        })
        .collect();

    serde_json::to_string(&out).unwrap_or_else(|_| "[]".to_string())
}
