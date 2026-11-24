use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Deserialize)]
pub struct AppSearchInput {
    pub package_name: String,
    pub label: String,
    #[serde(default)]
    pub launch_count: i32,
}

#[derive(Debug, Clone, Serialize)]
pub struct AppSearchHit {
    pub package_name: String,
    pub score: i32,
}

fn score_match(label_lower: &str, pkg_lower: &str, query: &str, launch_count: i32) -> Option<i32> {
    if query.is_empty() {
        return Some(launch_count.min(500));
    }

    let base: i32 = if label_lower == query {
        10_000
    } else if label_lower.starts_with(query) {
        8_000 - (label_lower.len() as i32 - query.len() as i32).max(0) * 5
    } else if label_lower.split_whitespace().any(|w| w.starts_with(query)) {
        6_500
    } else if let Some(idx) = label_lower.find(query) {
        5_000 - (idx as i32) * 10
    } else {
        let initials: String = label_lower
            .split(|c: char| !c.is_alphanumeric())
            .filter_map(|w| w.chars().next())
            .collect();
        if initials.starts_with(query) {
            4_500
        } else if let Some(sub_score) = subsequence_score(label_lower, query) {
            2_500 + sub_score
        } else if pkg_lower.contains(query) {
            1_500
        } else {
            return None;
        }
    };

    let frecency_bonus = (launch_count.max(0)).min(200) * 4;
    Some(base + frecency_bonus)
}

fn subsequence_score(text: &str, query: &str) -> Option<i32> {
    let mut q_chars = query.chars().peekable();
    let mut consecutive = 0i32;
    let mut best_bonus = 0i32;
    let mut last_matched = false;

    for ch in text.chars() {
        if let Some(&q) = q_chars.peek() {
            if ch == q {
                q_chars.next();
                if last_matched {
                    consecutive += 1;
                    best_bonus += consecutive * 15;
                } else {
                    consecutive = 1;
                    best_bonus += 10;
                }
                last_matched = true;
            } else {
                last_matched = false;
            }
        }
    }

    if q_chars.peek().is_none() {
        Some(best_bonus)
    } else {
        None
    }
}

pub fn fuzzy_search_json(apps_json: &str, query: &str) -> String {
    let apps: Vec<AppSearchInput> = match serde_json::from_str(apps_json) {
        Ok(v) => v,
        Err(_) => return "[]".to_string(),
    };

    let q = query.trim().to_lowercase();
    let mut hits: Vec<(AppSearchHit, String)> = apps
        .into_iter()
        .filter_map(|app| {
            let label_lower = app.label.to_lowercase();
            let pkg_lower = app.package_name.to_lowercase();
            let score = score_match(&label_lower, &pkg_lower, &q, app.launch_count)?;
            Some((
                AppSearchHit {
                    package_name: app.package_name,
                    score,
                },
                label_lower,
            ))
        })
        .collect();

    hits.sort_by(|a, b| b.0.score.cmp(&a.0.score).then_with(|| a.1.cmp(&b.1)));
    let result: Vec<AppSearchHit> = hits.into_iter().map(|(h, _)| h).collect();
    serde_json::to_string(&result).unwrap_or_else(|_| "[]".to_string())
}

#[derive(Debug, Clone, PartialEq)]
enum Token {
    Num(f64),
    Plus,
    Minus,
    Star,
    Slash,
    Caret,
    Percent,
    LParen,
    RParen,
}

fn tokenize(expr: &str) -> Option<Vec<Token>> {
    let mut tokens = Vec::new();
    let chars: Vec<char> = expr.chars().collect();
    let mut i = 0;
    let mut has_operator = false;

    while i < chars.len() {
        let c = chars[i];
        if c.is_whitespace() {
            i += 1;
            continue;
        }
        if c.is_ascii_digit() || c == '.' {
            let start = i;
            let mut dot_count = 0;
            while i < chars.len() && (chars[i].is_ascii_digit() || chars[i] == '.') {
                if chars[i] == '.' {
                    dot_count += 1;
                    if dot_count > 1 {
                        return None;
                    }
                }
                i += 1;
            }
            let num_str: String = chars[start..i].iter().collect();
            let val: f64 = num_str.parse().ok()?;
            tokens.push(Token::Num(val));
            continue;
        }
        match c {
            '+' => { has_operator = true; tokens.push(Token::Plus); }
            '-' => { has_operator = true; tokens.push(Token::Minus); }
            '*' | 'x' | 'X' | '×' => { has_operator = true; tokens.push(Token::Star); }
            '/' | '÷' => { has_operator = true; tokens.push(Token::Slash); }
            '^' => { has_operator = true; tokens.push(Token::Caret); }
            '%' => { has_operator = true; tokens.push(Token::Percent); }
            '(' => tokens.push(Token::LParen),
            ')' => tokens.push(Token::RParen),
            _ => return None,
        }
        i += 1;
    }

    if tokens.is_empty() || !has_operator {
        return None;
    }
    Some(tokens)
}

struct Parser<'a> {
    tokens: &'a [Token],
    pos: usize,
}

impl<'a> Parser<'a> {
    fn peek(&self) -> Option<&Token> {
        self.tokens.get(self.pos)
    }

    fn next(&mut self) -> Option<Token> {
        let t = self.tokens.get(self.pos).cloned();
        if t.is_some() {
            self.pos += 1;
        }
        t
    }

    fn parse_expr(&mut self) -> Option<f64> {
        let mut val = self.parse_term()?;
        while let Some(tok) = self.peek() {
            match tok {
                Token::Plus => {
                    self.next();
                    val += self.parse_term()?;
                }
                Token::Minus => {
                    self.next();
                    val -= self.parse_term()?;
                }
                _ => break,
            }
        }
        Some(val)
    }

    fn parse_term(&mut self) -> Option<f64> {
        let mut val = self.parse_power()?;
        while let Some(tok) = self.peek() {
            match tok {
                Token::Star => {
                    self.next();
                    val *= self.parse_power()?;
                }
                Token::Slash => {
                    self.next();
                    let rhs = self.parse_power()?;
                    if rhs.abs() < 1e-12 {
                        return None;
                    }
                    val /= rhs;
                }
                _ => break,
            }
        }
        Some(val)
    }

    fn parse_power(&mut self) -> Option<f64> {
        let base = self.parse_unary()?;
        if matches!(self.peek(), Some(Token::Caret)) {
            self.next();
            let exp = self.parse_power()?;
            let res = base.powf(exp);
            if !res.is_finite() {
                return None;
            }
            Some(res)
        } else {
            Some(base)
        }
    }

    fn parse_unary(&mut self) -> Option<f64> {
        match self.peek() {
            Some(Token::Minus) => {
                self.next();
                Some(-self.parse_unary()?)
            }
            Some(Token::Plus) => {
                self.next();
                self.parse_unary()
            }
            _ => self.parse_postfix(),
        }
    }

    fn parse_postfix(&mut self) -> Option<f64> {
        let mut val = self.parse_primary()?;
        while matches!(self.peek(), Some(Token::Percent)) {
            self.next();
            val /= 100.0;
        }
        Some(val)
    }

    fn parse_primary(&mut self) -> Option<f64> {
        match self.next()? {
            Token::Num(n) => Some(n),
            Token::LParen => {
                let inner = self.parse_expr()?;
                if matches!(self.next(), Some(Token::RParen)) {
                    Some(inner)
                } else {
                    None
                }
            }
            _ => None,
        }
    }
}

pub fn evaluate_math_expr(expr: &str) -> String {
    let tokens = match tokenize(expr) {
        Some(t) => t,
        None => return String::new(),
    };
    let mut parser = Parser {
        tokens: &tokens,
        pos: 0,
    };
    let val = match parser.parse_expr() {
        Some(v) if parser.pos == tokens.len() && v.is_finite() => v,
        _ => return String::new(),
    };

    if (val - val.round()).abs() < 1e-9 && val.abs() < 1e15 {
        format!("{}", val.round() as i64)
    } else {
        let formatted = format!("{:.6}", val);
        formatted
            .trim_end_matches('0')
            .trim_end_matches('.')
            .to_string()
    }
}
