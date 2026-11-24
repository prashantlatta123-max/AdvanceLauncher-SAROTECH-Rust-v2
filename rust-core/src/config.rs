use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(default)]
pub struct LauncherConfig {
    pub theme_preset: String,
    pub accent_hex: String,
    pub glass_opacity: f32,
    pub neo_depth: f32,
    pub desktop_cols: i32,
    pub desktop_rows: i32,
    pub drawer_cols: i32,
    pub drawer_style: String,
    pub drawer_sort: String,
    pub dock_enabled: bool,
    pub dock_slots: i32,
    pub dock_style: String,
    pub icon_size_dp: i32,
    pub icon_shape: String,
    pub icon_pack_package: String,
    pub badge_style: String,
    pub show_desktop_labels: bool,
    pub show_dock_labels: bool,
    pub show_drawer_labels: bool,
    pub label_size_sp: i32,
    pub clock_style: String,
    pub search_bar_placement: String,
    pub gesture_swipe_up: String,
    pub gesture_swipe_down: String,
    pub gesture_double_tap: String,
    pub page_transition: String,
    pub hidden_packages: Vec<String>,
}

impl Default for LauncherConfig {
    fn default() -> Self {
        Self {
            theme_preset: "GlassDark".to_string(),
            accent_hex: "#4FC3F7".to_string(),
            glass_opacity: 0.52,
            neo_depth: 1.0,
            desktop_cols: 4,
            desktop_rows: 5,
            drawer_cols: 4,
            drawer_style: "Grid".to_string(),
            drawer_sort: "AZ".to_string(),
            dock_enabled: true,
            dock_slots: 5,
            dock_style: "Floating".to_string(),
            icon_size_dp: 52,
            icon_shape: "Squircle".to_string(),
            icon_pack_package: String::new(),
            badge_style: "Count".to_string(),
            show_desktop_labels: true,
            show_dock_labels: false,
            show_drawer_labels: true,
            label_size_sp: 12,
            clock_style: "DigitalModern".to_string(),
            search_bar_placement: "Top".to_string(),
            gesture_swipe_up: "OpenDrawer".to_string(),
            gesture_swipe_down: "ExpandNotifications".to_string(),
            gesture_double_tap: "LockScreen".to_string(),
            page_transition: "Depth".to_string(),
            hidden_packages: Vec::new(),
        }
    }
}

fn valid_choice(val: &str, allowed: &[&str], fallback: &str) -> String {
    if allowed.iter().any(|&a| a == val) {
        val.to_string()
    } else {
        fallback.to_string()
    }
}

fn valid_hex_color(val: &str) -> String {
    let trimmed = val.trim();
    if trimmed.len() == 7
        && trimmed.starts_with('#')
        && trimmed[1..].chars().all(|c| c.is_ascii_hexdigit())
    {
        trimmed.to_uppercase()
    } else {
        "#4FC3F7".to_string()
    }
}

impl LauncherConfig {
    pub fn normalized(mut self) -> Self {
        self.theme_preset = valid_choice(
            &self.theme_preset,
            &["GlassDark", "GlassLight", "NeomorphDark", "NeomorphLight"],
            "GlassDark",
        );
        self.accent_hex = valid_hex_color(&self.accent_hex);
        self.glass_opacity = self.glass_opacity.clamp(0.18, 0.92);
        self.neo_depth = self.neo_depth.clamp(0.4, 1.8);
        self.desktop_cols = self.desktop_cols.clamp(3, 8);
        self.desktop_rows = self.desktop_rows.clamp(4, 10);
        self.drawer_cols = self.drawer_cols.clamp(3, 7);
        self.drawer_style = valid_choice(&self.drawer_style, &["Grid", "List", "Categories"], "Grid");
        self.drawer_sort = valid_choice(&self.drawer_sort, &["AZ", "ZA", "MostUsed"], "AZ");
        self.dock_slots = self.dock_slots.clamp(3, 7);
        self.dock_style = valid_choice(
            &self.dock_style,
            &["Floating", "DockedBar", "Transparent"],
            "Floating",
        );
        self.icon_size_dp = self.icon_size_dp.clamp(36, 72);
        self.icon_shape = valid_choice(
            &self.icon_shape,
            &["Circle", "Squircle", "RoundedSquare", "Teardrop", "Hexagon"],
            "Squircle",
        );
        self.icon_pack_package = self.icon_pack_package.trim().to_string();
        self.badge_style = valid_choice(&self.badge_style, &["Count", "Dot", "Off"], "Count");
        self.label_size_sp = self.label_size_sp.clamp(10, 16);
        self.clock_style = valid_choice(
            &self.clock_style,
            &["DigitalModern", "MinimalPill", "AnalogClean", "Hidden"],
            "DigitalModern",
        );
        self.search_bar_placement =
            valid_choice(&self.search_bar_placement, &["Top", "Bottom", "Hidden"], "Top");

        let gestures = &[
            "OpenDrawer",
            "OpenSearch",
            "ExpandNotifications",
            "LockScreen",
            "OpenSettings",
            "None",
        ];
        self.gesture_swipe_up = valid_choice(&self.gesture_swipe_up, gestures, "OpenDrawer");
        self.gesture_swipe_down =
            valid_choice(&self.gesture_swipe_down, gestures, "ExpandNotifications");
        self.gesture_double_tap = valid_choice(&self.gesture_double_tap, gestures, "LockScreen");
        self.page_transition = valid_choice(
            &self.page_transition,
            &["Slide", "Depth", "Zoom", "Fade"],
            "Depth",
        );

        self.hidden_packages.sort();
        self.hidden_packages.dedup();
        self
    }
}

pub fn normalize_config_json(raw: &str) -> String {
    let parsed: LauncherConfig = serde_json::from_str(raw).unwrap_or_default();
    let clean = parsed.normalized();
    serde_json::to_string(&clean).unwrap_or_else(|_| "{}".to_string())
}
