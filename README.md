# ChatManagement 2

**Advanced Anti-Spam & Chat Protection Plugin for Minecraft 1.21.x**

ChatManagement 2 is a complete rewrite of the original ChatProtection plugin, featuring improved spam detection algorithms, fewer false positives, and enhanced performance. Fully compatible with Folia's region-based threading.

---

## 🎯 Key Features

### Smart Spam Detection
- **Similarity Matching** - Detects spam even with typos using an improved Levenshtein distance algorithm
  - `"hello everyone"` vs `"helo everyone"` → Blocked if too similar
  - Configurable similarity threshold (default: 80%)
  - Length-based filtering prevents false positives on short messages

### Blocked Words System
- **Intelligent Pattern Matching** - Automatically detects obfuscations:
  - Leetspeak: `f*ck`, `fvck`, `fu<k`
  - Character substitutions: `@→a`, `$→s`, `0→o`, `3→e`
  - Spacing tricks: `f u c k`
  - Special characters: `f.u.c.k`, `f-u-c-k`
- **Partial Match Control** - Optional partial word blocking with minimum length requirements
- **False Positive Prevention** - Smart boundary detection prevents blocking legitimate words

### Anti-Spam Protection
- **Duplicate Prevention** - Blocks repeated messages with configurable threshold
- **Rapid Spam Detection** - Auto-kicks players sending too many messages too quickly (default: 7 msgs in 5 sec)
- **Rate Limiting** - Time-based message tracking per player

### Auto-Mute System
- **Repeat Offender Detection** - Automatically mutes players who get spam-kicked repeatedly (default: 3 kicks in 10 min = 5 min mute)
- **Persistent Storage** - Mutes survive server restarts (database or YAML)
- **Smart Timer** - Pauses when player disconnects, resumes on rejoin
- **Configurable Durations** - Fully customizable thresholds and durations

### Private Messaging
- **Built-in Commands** - `/msg`, `/w`, `/tell`, `/pm`, `/dm`, `/whisper`
- **Reply System** - `/r` and `/reply` to quickly respond
- **Color Customization** - Private messages in custom colors (default: magenta)
- **Spam Protection** - All anti-spam checks apply to PMs
- **Mute Compatibility** - Optional setting to allow muted players to receive (but not send) PMs

### Chat Enhancement
- **Colored Messages** - Prefix messages with `>` for custom color (default: light green)
- **Silent Blocking** - Configurable notifications when messages are blocked
- **Permission Bypass** - Trusted players skip all checks
- **Customizable Messages** - All plugin messages can be customized

### Technical Excellence
- **Folia Compatible** - Full support for region-based threading
- **High Performance** - Minimal overhead (<1ms per message)
- **Database Support** - SQLite or MySQL for persistent storage
- **Hot Reload** - `/cm reload` updates config without restart
- **Comprehensive Logging** - Debug and verbose modes for troubleshooting

---

## 📦 Installation

1. Download the latest `ChatManagement2-1.0.0.jar`
2. Place in your server's `plugins` folder
3. Start/restart your server
4. Configure `plugins/ChatManagement2/config.yml` to your needs
5. Run `/cm reload` to apply changes

### Requirements
- **Minecraft Server**: 1.21.x (Paper, Folia, or compatible fork)
- **Java**: 21 or higher

---

## ⚙️ Configuration

### Basic Setup

The default configuration works well out-of-the-box, but you can customize everything:

```yaml
# Core spam detection
settings:
  similarity-threshold: 80  # 0-100%, higher = more strict
  message-history-size: 10  # Messages to track per player

# Duplicate message blocking
duplicate-messages:
  max-repeats: 2            # Allow 2 repeats before blocking
  cooldown-seconds: 30      # Clear history after this time

# Rapid spam detection
anti-spam-kick:
  enabled: true
  message-threshold: 7      # Messages in time window = kick
  time-window-seconds: 5

# Auto-mute for repeat offenders
auto-mute:
  enabled: true
  kick-threshold: 3         # Kicks in window = mute
  kick-window-minutes: 10
  mute-duration-seconds: 300
```

### Advanced Configuration

#### Database Setup

For better performance with many mutes, enable database storage:

**SQLite (Recommended for most servers)**:
```yaml
database:
  enabled: true
  type: sqlite
```

**MySQL (For large servers)**:
```yaml
database:
  enabled: true
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: chatmanagement
    username: your_username
    password: your_password
```

#### Blocked Words

Configure the word filter to your needs:

```yaml
blocked-words:
  enabled: true
  block-partial-matches: false  # Prevent false positives
  min-word-length: 4            # Minimum length for partial matching
  word-list:
    - badword1
    - badword2
```

#### Custom Messages

All messages can be customized and toggled:

```yaml
messages:
  notify-blocked-message: false  # Silent blocking
  blocked-message-notification: "&cYour message was blocked."
  
anti-spam-kick:
  notify-player: true
  kick-message: "&cYou have been kicked for spamming!"

auto-mute:
  notify-player: true
  mute-notification: "&cYou have been muted for &e{duration} &cseconds."
  mute-message: "&cYou are muted. Time remaining: &e{time} &cseconds."
```

#### Private Messaging

Customize PM appearance:

```yaml
private-messaging:
  enabled: true
  message-color: "&d"  # Magenta
  sent-format: "&7[&dYou &7-> &d{receiver}&7] &r{message}"
  received-format: "&7[&d{sender} &7-> &dYou&7] &r{message}"
```

#### Chat Colors

Enable colored chat feature:

```yaml
chat-colors:
  enabled: true
  color-prefix: ">"       # Type >message for colored text
  prefix-color: "&a"      # Light green
```

---

## 📝 Commands

### Main Commands
- `/chatmanagement` or `/cm` - Main plugin command
- `/cm reload` - Reload configuration
- `/cm help` - Show help message
- `/cm version` - Show plugin version

### Private Messaging
- `/msg <player> <message>` - Send a private message
- `/w <player> <message>` - Alias for /msg
- `/tell <player> <message>` - Alias for /msg
- `/pm <player> <message>` - Alias for /msg
- `/dm <player> <message>` - Alias for /msg
- `/whisper <player> <message>` - Alias for /msg
- `/r <message>` - Reply to last message
- `/reply <message>` - Alias for /r

---

## 🔐 Permissions

- `chatmanagement.reload` - Allows reloading the plugin (default: op)
- `chatmanagement.bypass` - Bypass all chat restrictions (default: op)

---

## 🛠️ Building from Source

### Requirements
- Java 21 or higher
- Maven 3.8 or higher

### Build Steps

```bash
# Clone the repository
git clone https://github.com/yourusername/ChatManagement2.git
cd ChatManagement2

# Build with Maven
mvn clean package

# The compiled JAR will be in target/ChatManagement2-1.0.0.jar
```

---

## 🔧 Troubleshooting

### Too Many False Positives

If legitimate messages are being blocked:

1. **Increase similarity threshold**:
   ```yaml
   settings:
     similarity-threshold: 85  # Higher = more strict, fewer false positives
   ```

2. **Disable partial word matching**:
   ```yaml
   blocked-words:
     block-partial-matches: false
   ```

3. **Increase minimum message length**:
   ```yaml
   duplicate-messages:
     min-message-length: 5
   ```

### Messages Not Being Blocked

If spam is getting through:

1. **Decrease similarity threshold**:
   ```yaml
   settings:
     similarity-threshold: 75  # Lower = catches more spam
   ```

2. **Lower spam thresholds**:
   ```yaml
   anti-spam-kick:
     message-threshold: 5
   ```

3. **Enable debug mode**:
   ```yaml
   settings:
     debug: true
   ```
   Check console for detection information.

### Database Issues

If database connection fails:

1. Check credentials in config
2. Ensure database exists (for MySQL)
3. Plugin will automatically fall back to YAML storage
4. Check console for error messages

---

## 📊 Performance

ChatManagement 2 is designed for minimal impact:

- **Average overhead**: <1ms per message
- **Memory usage**: ~2-5 MB for typical servers
- **Database queries**: Optimized with prepared statements
- **Thread-safe**: Fully compatible with Folia's threading model

---

## 🆚 Differences from ChatProtection

ChatManagement 2 is a complete rewrite with major improvements:

1. **Better Accuracy** - Improved spam detection with fewer false positives
2. **Length-Based Filtering** - Prevents blocking short, legitimate messages
3. **Partial Match Control** - Smart boundary detection for blocked words
4. **Database Support** - Optional SQLite/MySQL for better performance
5. **Enhanced Mute System** - Pause/resume on disconnect/reconnect
6. **Improved Configuration** - More granular control over all features
7. **Better Documentation** - Comprehensive comments and examples

---

## 📄 License

This plugin is provided as-is for personal and commercial use on Minecraft servers.

---

## 📋 Changelog

### v1.0.0 (Initial Release)
- Complete rewrite of ChatProtection
- Improved spam detection algorithms
- Database support (SQLite/MySQL)
- Enhanced mute system with persistence
- Better configuration options
- Comprehensive documentation
- Folia compatibility
- Fewer false positives
- Performance optimizations

---

**Made with ❤️ for the Minecraft community**
