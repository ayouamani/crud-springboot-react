package com.crud.crud.application.controller;

import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/lab")
public class MemoryLabController {

    // ❌ Fuite typique : cache statique non borné
    private static final Map<String, byte[]> LEAKY_CACHE = new ConcurrentHashMap<>();

    @PostMapping("/leak/cache")
    public String leakCache(@RequestParam(defaultValue = "5") int mb,
                            @RequestParam(defaultValue = "true") boolean uniqueKeys) {
        String key = uniqueKeys ? Instant.now().toEpochMilli() + "-" + Math.random() : "fixed";
        LEAKY_CACHE.put(key, new byte[mb * 1024 * 1024]);
        return "cacheSize=" + LEAKY_CACHE.size() + " lastKey=" + key + " +" + mb + "MB";
    }

    // ✅ Correctif “propre” : on vide (ou mieux : cache borné, voir plus bas)
    @DeleteMapping("/fix/cache")
    public String clearCache() {
        int before = LEAKY_CACHE.size();
        LEAKY_CACHE.clear();
        return "cleared=" + before;
    }

    // ❌ ThreadLocal leak (si on ne remove pas sur un thread pool)
    private static final ThreadLocal<byte[]> TL = new ThreadLocal<>();

    @PostMapping("/leak/threadlocal")
    public String leakThreadLocal(@RequestParam(defaultValue = "10") int mb) {
        TL.set(new byte[mb * 1024 * 1024]);
        // pas de remove => fuite sur threads réutilisés par Tomcat
        return "threadLocal set +" + mb + "MB (no remove)";
    }

    @PostMapping("/fix/threadlocal")
    public String fixThreadLocal(@RequestParam(defaultValue = "10") int mb) {
        try {
            TL.set(new byte[mb * 1024 * 1024]);
            return "threadLocal set +" + mb + "MB (with remove)";
        } finally {
            TL.remove(); // ✅ fix
        }
    }

}
