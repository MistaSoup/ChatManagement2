package com.chatmanagement.managers;

import com.chatmanagement.ChatManagement2;

import java.util.*;
import java.util.regex.Pattern;

public class BlockedWordsManager {
    
    private final ChatManagement2 plugin;
    private final Set<String> blockedWords;
    private final Map<Character, List<Character>> characterSubstitutions;
    private final Set<Pattern> blockedPatterns;
    
    public BlockedWordsManager(ChatManagement2 plugin) {
        this.plugin = plugin;
        this.blockedWords = new HashSet<>();
        this.characterSubstitutions = new HashMap<>();
        this.blockedPatterns = new HashSet<>();
        
        initializeSubstitutions();
        reload();
    }
    
    public void reload() {
        blockedWords.clear();
        blockedPatterns.clear();
        
        if (!plugin.getConfigManager().isBlockedWordsEnabled()) {
            return;
        }
        
        List<String> words = plugin.getConfigManager().getBlockedWords();
        for (String word : words) {
            blockedWords.add(word.toLowerCase());
            
            // Create regex pattern for this word with common substitutions
            String pattern = createFlexiblePattern(word.toLowerCase());
            blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
        }
        
        if (plugin.getConfigManager().isDebugEnabled()) {
            plugin.getLogger().info("Loaded " + blockedWords.size() + " blocked words");
        }
    }
    
    /**
     * Initialize common character substitutions for leetspeak and obfuscation
     */
    private void initializeSubstitutions() {
        characterSubstitutions.put('a', Arrays.asList('a', '@', '4', 'á', 'à', 'â', 'ä', 'å'));
        characterSubstitutions.put('e', Arrays.asList('e', '3', 'é', 'è', 'ê', 'ë'));
        characterSubstitutions.put('i', Arrays.asList('i', '1', '!', 'í', 'ì', 'î', 'ï'));
        characterSubstitutions.put('o', Arrays.asList('o', '0', 'ó', 'ò', 'ô', 'ö'));
        characterSubstitutions.put('u', Arrays.asList('u', 'ú', 'ù', 'û', 'ü'));
        characterSubstitutions.put('s', Arrays.asList('s', '$', '5', 'z'));
        characterSubstitutions.put('t', Arrays.asList('t', '7', '+'));
        characterSubstitutions.put('l', Arrays.asList('l', '1', '|'));
        characterSubstitutions.put('g', Arrays.asList('g', '9', 'q'));
        characterSubstitutions.put('b', Arrays.asList('b', '8'));
        characterSubstitutions.put('c', Arrays.asList('c', '(', 'k'));
        characterSubstitutions.put('k', Arrays.asList('k', 'c'));
    }
    
    /**
     * Check if message contains blocked words
     * Returns true if message should be blocked
     */
    public boolean containsBlockedWord(String message) {
        if (!plugin.getConfigManager().isBlockedWordsEnabled()) {
            return false;
        }
        
        String normalized = normalizeMessage(message);
        
        // Check against patterns
        for (Pattern pattern : blockedPatterns) {
            if (pattern.matcher(normalized).find()) {
                if (plugin.getConfigManager().isDebugEnabled()) {
                    plugin.getLogger().info("Blocked word pattern matched in: " + message);
                }
                return true;
            }
        }
        
        // Additional check for partial matches if enabled
        if (plugin.getConfigManager().shouldBlockPartialMatches()) {
            return containsPartialMatch(normalized);
        }
        
        return false;
    }
    
    /**
     * Check for partial word matches in message
     */
    private boolean containsPartialMatch(String message) {
        String[] words = message.split("\\s+");
        int minLength = plugin.getConfigManager().getMinWordLength();
        
        for (String word : words) {
            if (word.length() < minLength) {
                continue;
            }
            
            for (String blocked : blockedWords) {
                // Only check if blocked word is substantial enough
                if (blocked.length() >= minLength) {
                    // Check if the word contains the blocked word
                    if (word.contains(blocked)) {
                        // Additional check: make sure it's not a false positive
                        // e.g., "classic" shouldn't match "ass"
                        if (isLikelyTrueMatch(word, blocked)) {
                            if (plugin.getConfigManager().isDebugEnabled()) {
                                plugin.getLogger().info("Partial blocked word match: '" + blocked + "' in '" + word + "'");
                            }
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    /**
     * Check if a partial match is likely a true match and not a false positive
     */
    private boolean isLikelyTrueMatch(String word, String blocked) {
        // If the blocked word is the entire word, definitely a match
        if (word.equals(blocked)) {
            return true;
        }
        
        // If blocked word is very short, be more strict
        if (blocked.length() <= 3) {
            return word.equals(blocked);
        }
        
        // Check if blocked word appears as a clear substring
        // Not at the end/start of a longer word that might be legitimate
        int index = word.indexOf(blocked);
        if (index == -1) {
            return false;
        }
        
        // If it's at the start or end, check the character boundaries
        if (index == 0) {
            // At start - check if next char would make it part of a larger word
            if (word.length() > blocked.length()) {
                char nextChar = word.charAt(blocked.length());
                // If followed by vowel, might be part of legitimate word
                if ("aeiou".indexOf(nextChar) != -1) {
                    return false;
                }
            }
            return true;
        }
        
        if (index + blocked.length() == word.length()) {
            // At end - similar check
            char prevChar = word.charAt(index - 1);
            if ("aeiou".indexOf(prevChar) != -1) {
                return false;
            }
            return true;
        }
        
        // In the middle - likely a match if it's clearly separated
        return true;
    }
    
    /**
     * Create a flexible regex pattern that matches common obfuscations
     */
    private String createFlexiblePattern(String word) {
        StringBuilder pattern = new StringBuilder();
        
        // Allow optional non-alphanumeric characters between letters
        pattern.append("(?:");
        
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            
            if (i > 0) {
                // Allow 0-2 special characters or spaces between letters
                pattern.append("[^a-z0-9]{0,2}");
            }
            
            // Add character class for this letter with substitutions
            pattern.append("[");
            pattern.append(c);
            
            List<Character> subs = characterSubstitutions.get(c);
            if (subs != null) {
                for (Character sub : subs) {
                    if (sub != c) {
                        pattern.append(Pattern.quote(sub.toString()));
                    }
                }
            }
            
            pattern.append("]");
        }
        
        pattern.append(")");
        
        return pattern.toString();
    }
    
    /**
     * Normalize message for checking
     */
    private String normalizeMessage(String message) {
        return message.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }
    
    /**
     * Get list of blocked words (for admin purposes)
     */
    public Set<String> getBlockedWords() {
        return new HashSet<>(blockedWords);
    }
}
