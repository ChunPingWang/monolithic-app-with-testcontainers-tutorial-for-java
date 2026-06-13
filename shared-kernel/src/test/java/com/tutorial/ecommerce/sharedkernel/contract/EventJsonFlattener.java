package com.tutorial.ecommerce.sharedkernel.contract;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 把 JSON 樹攤平成「欄位路徑」清單,
 * 例:`{"order":{"id":"abc"}}` → `["order.id"]`,
 *     `{"lines":[{"qty":1}]}` → `["lines.[].qty"]`
 *
 * 用來判斷事件序列化後的欄位結構是否與「已公布的契約」一致。
 */
final class EventJsonFlattener {

    private EventJsonFlattener() {}

    static List<String> fieldPaths(JsonNode node) {
        var paths = new ArrayList<String>();
        walk("", node, paths);
        return paths;
    }

    private static void walk(String prefix, JsonNode node, List<String> out) {
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(name -> {
                var p = prefix.isEmpty() ? name : prefix + "." + name;
                walk(p, node.get(name), out);
            });
        } else if (node.isArray()) {
            var p = prefix + ".[]";
            if (node.isEmpty()) {
                out.add(p);   // 空陣列至少記一筆
            } else {
                walk(p, node.get(0), out);   // 取第一個 element 當代表
            }
        } else {
            out.add(prefix);   // primitive / null
        }
    }
}
