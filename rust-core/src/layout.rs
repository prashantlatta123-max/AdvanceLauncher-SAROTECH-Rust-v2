use serde::{Deserialize, Serialize};
use std::collections::HashSet;

#[derive(Debug, Clone, Deserialize, Serialize)]
pub struct GridRect {
    pub page: i32,
    pub col: i32,
    pub row: i32,
    #[serde(default = "default_span")]
    pub col_span: i32,
    #[serde(default = "default_span")]
    pub row_span: i32,
}

fn default_span() -> i32 {
    1
}

/// Finds the first grid coordinate `(page, col, row)` where a rectangle of
/// `col_span x row_span` fits cleanly without overlapping any occupied rect.
pub fn find_next_free_rect_json(
    occupied_json: &str,
    cols: i32,
    rows: i32,
    col_span: i32,
    row_span: i32,
) -> String {
    let safe_cols = cols.clamp(3, 8);
    let safe_rows = rows.clamp(4, 10);
    let w = col_span.clamp(1, safe_cols);
    let h = row_span.clamp(1, safe_rows);

    let occupied: Vec<GridRect> = serde_json::from_str(occupied_json).unwrap_or_default();
    let mut used: HashSet<(i32, i32, i32)> = HashSet::new();

    for rect in occupied {
        let rw = rect.col_span.max(1);
        let rh = rect.row_span.max(1);
        for dr in 0..rh {
            for dc in 0..rw {
                used.insert((rect.page, rect.col + dc, rect.row + dr));
            }
        }
    }

    for page in 0..10 {
        for row in 0..=(safe_rows - h) {
            for col in 0..=(safe_cols - w) {
                let mut fits = true;
                'check: for dr in 0..h {
                    for dc in 0..w {
                        if used.contains(&(page, col + dc, row + dr)) {
                            fits = false;
                            break 'check;
                        }
                    }
                }
                if fits {
                    let free = GridRect {
                        page,
                        col,
                        row,
                        col_span: w,
                        row_span: h,
                    };
                    return serde_json::to_string(&free).unwrap_or_else(|_| {
                        "{"page":0,"col":0,"row":0,"col_span":1,"row_span":1}".to_string()
                    });
                }
            }
        }
    }

    format!(
        "{{"page":0,"col":0,"row":0,"col_span":{},"row_span":{}}}",
        w, h
    )
}
