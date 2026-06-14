package com.zwei.iot.timeseries.domain;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 表达式渲染器 — 把 {@link ExpressionSpec} 树转为 IoTDB SQL 字符串 + 别名。
 *
 * <p>渲染方法 {@link #render} 递归遍历表达式树,拼出 IoTDB 表达式片段;
 * {@link #alias} 同步生成可作为 ResultSet 列名 + 返回 Map key 的别名。</p>
 */
@Component
public class ExpressionSpecRenderer {

    private static final int MAX_DEPTH = 5;
    private static final int MAX_ALIAS_LENGTH = 64;
    private static final Pattern ALIAS_ALLOWED = Pattern.compile("[a-zA-Z0-9_\\-()/.]*");

    /**
     * 递归渲染为 IoTDB 表达式。
     *
     * @param expr     表达式树
     * @param attrCode 业务指标编码
     * @return IoTDB 表达式字符串,带括号
     * @throws IllegalArgumentException 嵌套深度超过 5 层
     */
    public String render(ExpressionSpec expr, String attrCode) {
        return render(expr, attrCode, 0);
    }

    private String render(ExpressionSpec expr, String attrCode, int depth) {
        if (depth > MAX_DEPTH) {
            throw new IllegalArgumentException("表达式嵌套过深 (max=" + MAX_DEPTH + ")");
        }
        if (expr instanceof ExpressionSpec.FunctionCall fc) {
            return fc.func().getIotdbExpr(attrCode);
        } else if (expr instanceof ExpressionSpec.Constant c) {
            return String.valueOf(c.value());
        } else if (expr instanceof ExpressionSpec.BinaryOp bo) {
            return "(" + render(bo.left(), attrCode, depth + 1)
                    + " " + bo.op().getSymbol() + " "
                    + render(bo.right(), attrCode, depth + 1) + ")";
        }
        throw new IllegalArgumentException("未知 ExpressionSpec: " + expr.getClass());
    }

    /**
     * 生成别名(用于 SELECT AS 别名 + 返回 Map key)。
     *
     * <p>特殊映射: {@code LAST_VALUE - FIRST_VALUE} → {@code DELTA}。
     * 别名仅允许字母数字 + {@code _-/()},长度 ≤ 64。</p>
     *
     * @throws IllegalArgumentException 别名格式非法或超长
     */
    public String alias(ExpressionSpec expr) {
        String alias = doAlias(expr);
        // Strip outer parentheses for top-level BinaryOp
        if (expr instanceof ExpressionSpec.BinaryOp && alias.startsWith("(") && alias.endsWith(")")) {
            alias = alias.substring(1, alias.length() - 1);
        }
        if (alias.length() > MAX_ALIAS_LENGTH) {
            throw new IllegalArgumentException("别名过长 (max=" + MAX_ALIAS_LENGTH + "): " + alias);
        }
        if (!ALIAS_ALLOWED.matcher(alias).matches()) {
            throw new IllegalArgumentException("别名含非法字符: " + alias);
        }
        return alias;
    }

    private String doAlias(ExpressionSpec expr) {
        if (expr instanceof ExpressionSpec.FunctionCall fc) {
            return fc.func().name();
        } else if (expr instanceof ExpressionSpec.Constant c) {
            return String.valueOf(c.value());
        } else if (expr instanceof ExpressionSpec.BinaryOp bo) {
            String left = doAlias(bo.left());
            String right = doAlias(bo.right());
            if (bo.op() == ExpressionSpec.BinaryOperator.SUB
                    && "LAST_VALUE".equals(left) && "FIRST_VALUE".equals(right)) {
                return "DELTA";
            }
            return "(" + left + bo.op().getSymbol() + right + ")";
        }
        throw new IllegalArgumentException("未知 ExpressionSpec: " + expr.getClass());
    }
}
