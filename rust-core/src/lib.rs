mod backup;
mod categorizer;
mod config;
mod layout;
mod search;

use jni::objects::{JClass, JString};
use jni::sys::{jint, jstring};
use jni::JNIEnv;
use std::panic::{catch_unwind, AssertUnwindSafe};

fn read_jstring<'local>(env: &mut JNIEnv<'local>, input: &JString<'local>) -> String {
    env.get_string(input).map(String::from).unwrap_or_default()
}

fn to_jstring<'local>(env: &mut JNIEnv<'local>, output: &str) -> jstring {
    env.new_string(output)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

fn safe_run<F: FnOnce() -> String>(fallback: &str, f: F) -> String {
    catch_unwind(AssertUnwindSafe(f)).unwrap_or_else(|_| fallback.to_string())
}

#[no_mangle]
pub extern "system" fn Java_com_sarotech_advancelauncher_core_RustEngine_getEngineVersion<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> jstring {
    to_jstring(
        &mut env,
        "Advance Core v2.0.0 (Rust Native • SAROTECH)",
    )
}

#[no_mangle]
pub extern "system" fn Java_com_sarotech_advancelauncher_core_RustEngine_normalizeConfig<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    config_json: JString<'local>,
) -> jstring {
    let raw = read_jstring(&mut env, &config_json);
    let out = safe_run("{}", || config::normalize_config_json(&raw));
    to_jstring(&mut env, &out)
}

#[no_mangle]
pub extern "system" fn Java_com_sarotech_advancelauncher_core_RustEngine_fuzzySearchApps<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    apps_json: JString<'local>,
    query: JString<'local>,
) -> jstring {
    let apps = read_jstring(&mut env, &apps_json);
    let q = read_jstring(&mut env, &query);
    let out = safe_run("[]", || search::fuzzy_search_json(&apps, &q));
    to_jstring(&mut env, &out)
}

#[no_mangle]
pub extern "system" fn Java_com_sarotech_advancelauncher_core_RustEngine_evaluateMath<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    expression: JString<'local>,
) -> jstring {
    let expr = read_jstring(&mut env, &expression);
    let out = safe_run("", || search::evaluate_math_expr(&expr));
    to_jstring(&mut env, &out)
}

#[no_mangle]
pub extern "system" fn Java_com_sarotech_advancelauncher_core_RustEngine_sortAndCategorizeApps<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    apps_json: JString<'local>,
    sort_mode: JString<'local>,
) -> jstring {
    let apps = read_jstring(&mut env, &apps_json);
    let mode = read_jstring(&mut env, &sort_mode);
    let out = safe_run("[]", || categorizer::sort_and_categorize_json(&apps, &mode));
    to_jstring(&mut env, &out)
}

#[no_mangle]
pub extern "system" fn Java_com_sarotech_advancelauncher_core_RustEngine_findNextFreeRect<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    occupied_json: JString<'local>,
    cols: jint,
    rows: jint,
    col_span: jint,
    row_span: jint,
) -> jstring {
    let occupied = read_jstring(&mut env, &occupied_json);
    let out = safe_run("{"page":0,"col":0,"row":0,"col_span":1,"row_span":1}", || {
        layout::find_next_free_rect_json(&occupied, cols, rows, col_span, row_span)
    });
    to_jstring(&mut env, &out)
}

#[no_mangle]
pub extern "system" fn Java_com_sarotech_advancelauncher_core_RustEngine_validateBackup<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    backup_json: JString<'local>,
) -> jstring {
    let raw = read_jstring(&mut env, &backup_json);
    let out = safe_run("{"ok":false,"error":"Core error"}", || {
        backup::validate_backup_json(&raw)
    });
    to_jstring(&mut env, &out)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_math_evaluator() {
        assert_eq!(search::evaluate_math_expr("2 + 2 * 5"), "12");
        assert_eq!(search::evaluate_math_expr("(1250 * 18%) + 450"), "675");
        assert_eq!(search::evaluate_math_expr("2^10"), "1024");
        assert_eq!(search::evaluate_math_expr("10 / 0"), "");
    }

    #[test]
    fn test_fuzzy_search() {
        let apps = r#"[
            {"package_name":"com.whatsapp","label":"WhatsApp","launch_count":15},
            {"package_name":"com.google.android.apps.maps","label":"Google Maps","launch_count":2}
        ]"#;
        assert!(search::fuzzy_search_json(apps, "gm").contains("com.google.android.apps.maps"));
        assert!(search::fuzzy_search_json(apps, "whtsp").contains("com.whatsapp"));
    }

    #[test]
    fn test_config_v2_fields() {
        let raw = r#"{"theme_preset":"NeomorphDark","badge_style":"Dot","gesture_double_tap":"LockScreen","icon_pack_package":"com.example.pack"}"#;
        let norm = config::normalize_config_json(raw);
        assert!(norm.contains(""theme_preset":"NeomorphDark""));
        assert!(norm.contains(""badge_style":"Dot""));
        assert!(norm.contains(""gesture_double_tap":"LockScreen""));
        assert!(norm.contains(""icon_pack_package":"com.example.pack""));
    }

    #[test]
    fn test_free_rect_widgets() {
        // Occupy top 2 rows completely with a 4x2 widget on a 4x5 grid
        let occ = r#"[{"page":0,"col":0,"row":0,"col_span":4,"row_span":2}]"#;
        let next = layout::find_next_free_rect_json(occ, 4, 5, 2, 2);
        assert!(next.contains(""row":2"));
        assert!(next.contains(""col":0"));
    }
}
