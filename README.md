# 🧮 GlassCalculator - Modern Glassmorphism Scientific Calculator & Currency Converter

<div align="center">

![Java](https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Swing](https://img.shields.io/badge/Swing-Desktop-4CAF50?style=for-the-badge)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apachemaven)
![Status](https://img.shields.io/badge/Status-Active-success?style=for-the-badge)

**A stunning desktop calculator with glassmorphism UI, scientific functions, currency conversion, and database-backed history.**

[Features](#-features) • [Screenshots](#-screenshots) • [Installation](#-installation) • [Usage](#-usage) • [Contributing](#-contributing)

</div>

---

## ✨ Features

### 🎨 Modern UI
- **Glassmorphism Design** - Beautiful frosted glass effect with smooth animations
- **Dark Theme** - Easy on the eyes with amber accents and cyan highlights
- **Responsive Layout** - Scales beautifully on different screen sizes
- **Smooth Animations** - Ripple effects, fade transitions, and toast notifications

### 🔢 Calculator Features
- **Basic Mode** - Standard arithmetic operations
- **Scientific Mode** - Advanced mathematical functions (trig, hyperbolic, roots, logarithms)
- **Memory Operations** - M+, M-, MC, MR for convenient calculations
- **Expression Evaluation** - Full parenthesis support, operator precedence, implicit multiplication
- **Keyboard Shortcuts** - Full keyboard support with visual feedback
- **Smart Display** - Dynamic font scaling for long expressions

### 🌍 Currency Converter
- **Live Exchange Rates** - Real-time currency conversion using ExchangeRate API
- **20+ Currencies** - USD, EUR, INR, GBP, JPY, AUD, CAD, CHF, CNY, RUB, BRL, ZAR, MXN, SGD, HKD, SEK, NOK, DKK, KRW, TRY
- **Dual Input** - Convert from/to any currency with swap functionality
- **Rate Caching** - 5-minute cache to reduce API calls
- **Debounced Conversion** - Smooth user experience with debounced updates

### 📊 History Management
- **Calculation History** - All calculations automatically saved
- **Conversion History** - Track all currency conversions
- **Dual Storage** - Saved in both Java Preferences and MySQL Database
- **Quick Access** - Easy history dialog with clear/export options
- **Persistent Storage** - History survives app restarts

### 🗄️ Database Integration
- **MySQL Support** - Automatic database creation and table management
- **Auto-reconnect** - Graceful reconnection with retry logic
- **Async Operations** - Non-blocking database saves
- **Thread-safe** - Concurrent access handling

### ⌨️ Keyboard Support
```
Enter / =        → Calculate
Backspace        → Delete character
Escape / Delete  → Clear (AC)
Ctrl+C / Ctrl+V  → Copy / Paste
Ctrl+A           → Select All
0-9, . , +*/-^   → Direct input
Right-click      → Context menu
```

---

## 📋 System Requirements

| Component | Version |
|-----------|---------|
| **Java** | 21 or higher |
| **Maven** | 3.6+ |
| **MySQL** | 5.7+ (optional, for history) |
| **OS** | Windows, macOS, Linux |
| **RAM** | 512 MB minimum |
| **Disk** | 50 MB |

---

## 🚀 Installation

### 1. Clone Repository
```bash
git clone https://github.com/guguluP/Calculator-Currency-Converter.git
cd Calculator-Currency-Converter
```

### 2. Build with Maven
```bash
mvn clean package
```

This creates a fat JAR with all dependencies bundled:
```
target/glass-calculator-1.0.jar
```

### 3. Run the Application

#### Quick Start (No Database)
```bash
java -jar target/glass-calculator-1.0.jar
```

#### With Database & Currency Conversion
```bash
# Set your credentials as environment variables
export MYSQL_PASS="your_mysql_password"
export EXCHANGE_RATE_API_KEY="your_api_key"

# Run the app
java -jar target/glass-calculator-1.0.jar
```

#### On Windows (PowerShell)
```powershell
$env:MYSQL_PASS = "your_password"
$env:EXCHANGE_RATE_API_KEY = "your_key"
java -jar target/glass-calculator-1.0.jar
```

#### On Windows (Command Prompt)
```cmd
set MYSQL_PASS=your_password
set EXCHANGE_RATE_API_KEY=your_key
java -jar target/glass-calculator-1.0.jar
```

### 4. Get API Key (Optional, for Currency Conversion)
1. Visit https://www.exchangerate-api.com/
2. Sign up for free account (100 requests/month)
3. Get your API key
4. Set `EXCHANGE_RATE_API_KEY` environment variable

### 5. Setup MySQL (Optional, for History)
```bash
# Start MySQL
mysql -u root -p

# Password will be read from MYSQL_PASS environment variable
```

The app automatically creates the database and tables on first run.

---

## 🎯 Usage

### Basic Calculations
1. Enter numbers and operators
2. Press `Enter` or click `=` to calculate
3. Results display with full precision

### Scientific Mode
1. Click menu (≡) → "Scientific Mode"
2. Access:
   - **Trigonometric**: sin, cos, tan (+ inverse modes)
   - **Hyperbolic**: sinh, cosh, tanh
   - **Power**: x², x³, x^y, 2^x
   - **Roots**: √, ∛, y√x
   - **Logarithms**: log (base 10), ln, log₂
   - **Constants**: π, e
   - **Factorial**: x!

### Currency Conversion
1. Click menu (≡) → "Currency Converter"
2. Enter amount in source currency
3. Select source and target currencies
4. Conversion happens automatically with live rates
5. Swap currencies with ↕ button
6. Results saved to history

### Memory Operations
- **M+** - Add display value to memory
- **M-** - Subtract from memory
- **MC** - Clear memory
- **MR** - Recall and insert memory value
- **Status** - Memory shows in status bar (💾 M: value)

### History
1. Click menu (≡) → "History"
2. View calculations and conversions
3. Clear individual tabs or all history
4. History persists across sessions

---

## 🏗️ Project Architecture

```
src/Project/
├── GlassCalculator.java        # Main UI window (1200+ lines)
├── model/
│   ├── ExpressionParser.java   # Math expression evaluator
│   └── UITheme.java            # Color scheme & fonts
├── service/
│   ├── DBManager.java          # MySQL integration
│   ├── CurrencyService.java    # Exchange rate API
│   └── (Future services)
├── ui/
│   ├── GlassButton.java        # Styled buttons with ripple effect
│   ├── ScalingDisplay.java     # Dynamic display area
│   ├── Toast.java              # Notification bubbles
│   ├── IconManager.java        # App icon handling
│   └── (UI components)
└── resources/
    └── AppIcon.png             # Application icon
```

### Key Components

| Class | Purpose | Lines |
|-------|---------|-------|
| **GlassCalculator** | Main application window | 1,300+ |
| **ExpressionParser** | Mathematical expression evaluation | 350+ |
| **DBManager** | Database operations | 120+ |
| **CurrencyService** | Exchange rate fetching | 180+ |
| **GlassButton** | Custom button with effects | 100+ |
| **UITheme** | Color constants and fonts | 30 |

---

## 🎨 UI Theme

### Colors
- **Background Deep**: `#0E0E14` - Main background
- **Surface**: `#181820` - Secondary panels
- **Elevated**: `#222230` - Elevated components
- **Glass Fill**: `rgba(255,255,255,0.11)` - Button fill
- **Accent Amber**: `#FFB91E` - Operators & emphasis
- **Accent Cyan**: `#3CC8FF` - Scientific buttons
- **Accent Green**: `#50DC8C` - Success
- **Accent Red**: `#FF5050` - Error

### Fonts
- **Display**: Segoe UI, 46pt (result display)
- **Button Large**: Segoe UI, 26pt
- **Button Small**: Segoe UI, 18pt
- **Status**: Segoe UI, 13pt

---

## 🔧 Configuration

### Environment Variables
```bash
# Required for currency conversion
EXCHANGE_RATE_API_KEY=your_api_key_here

# Required for MySQL database
MYSQL_PASS=your_mysql_password

# Optional: Custom MySQL host/user
MYSQL_HOST=localhost
MYSQL_USER=root
MYSQL_DB=mydb
```

### Build Configuration (pom.xml)
```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
</properties>
```

---

## 📊 Performance

| Metric | Value |
|--------|-------|
| **Startup Time** | < 2 seconds |
| **Memory Usage** | ~150 MB |
| **Expression Parse** | < 1 ms |
| **Currency Fetch** | ~500 ms (cached) |
| **UI Responsiveness** | 60 FPS |
| **Max Expression Length** | 500 characters |

---

## 🐛 Known Issues & Fixes

### ✅ Fixed in v2.1
- Security: Hardcoded credentials now use environment variables
- Parser: Unbalanced parentheses detection
- Thread safety: UI update synchronization
- Memory: Debouncer proper shutdown
- Input: Max length validation (500 chars)

### 📋 Roadmap

**v2.2 (Next Release)**
- [ ] Undo/Redo functionality
- [ ] Export history to CSV
- [ ] Dark/Light theme toggle
- [ ] Settings dialog (precision, history limit)
- [ ] Keyboard shortcut customization

**v2.3 (Future)**
- [ ] Unit converter (km/mi, kg/lb, etc.)
- [ ] Calculation graph visualization
- [ ] Voice input support
- [ ] Plugins/Extensions
- [ ] Multi-window support

---

## 🧪 Testing

### Run Tests
```bash
mvn test
```

### Manual Testing Checklist
- [ ] Unbalanced parentheses → Error
- [ ] 20! = 2,432,902,008,176,640,000
- [ ] asin(2) → Domain error
- [ ] 500+ character input → Toast warning
- [ ] Currency conversion works
- [ ] History persists across restart
- [ ] No memory leaks (check Task Manager)
- [ ] Keyboard input responsive

---

## 📈 Code Statistics

```
Total Lines of Code:     ~3,500
Java Files:              13
Documentation Lines:     ~500
Test Coverage:           Partial
Dependencies:            3 (MySQL, Swing, Preferences)
```

---

## 🔒 Security

### Best Practices Implemented
✅ **No Hardcoded Credentials** - All sensitive data from environment  
✅ **Input Validation** - All user input sanitized  
✅ **Thread Safety** - Concurrent access properly handled  
✅ **Resource Cleanup** - Proper connection/thread management  
✅ **Error Handling** - User-friendly error messages  
✅ **API Rate Limiting** - Cache prevents exhaustion  

### Security Recommendations
- Use strong MySQL password
- Keep API key confidential (don't commit to git)
- Run only from trusted sources
- Keep Java updated for security patches

---

## 🤝 Contributing

### Getting Started
1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Make changes and commit (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Code Style
- Follow Java naming conventions
- Use meaningful variable names
- Add comments for complex logic
- Keep methods focused and small (< 50 lines preferred)
- Use appropriate access modifiers

### Commit Messages
```
✨ Add new feature
🐛 Fix bug in X
📚 Update documentation
🔒 Security improvement
♻️ Code refactoring
⚡ Performance optimization
```

### Pull Request Process
1. Update README.md with any new features
2. Update pom.xml version number
3. Add test cases if applicable
4. Ensure code compiles: `mvn clean package`
5. Request review from maintainers

---

## 📜 License

This project is licensed under the **MIT License** - see [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2024-2026 guguluP

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 📚 Documentation

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - Detailed architecture overview
- [API_REFERENCE.md](docs/API_REFERENCE.md) - Class and method reference
- [CHANGELOG.md](CHANGELOG.md) - Version history
- [CONTRIBUTING.md](CONTRIBUTING.md) - Contribution guidelines

---

## 🎓 Educational Value

This project demonstrates:
- **GUI Development** - Modern Swing with custom rendering
- **Mathematical Parsing** - Recursive descent parser implementation
- **API Integration** - RESTful API client with caching
- **Database Design** - MySQL integration with connection pooling
- **Threading** - Concurrent operations with thread-safe components
- **UI/UX Design** - Glassmorphism and responsive layouts
- **Clean Code** - Well-organized, documented, maintainable codebase

Perfect for learning intermediate to advanced Java desktop development!

---

## 🙌 Acknowledgments

- **UI Design**: Glassmorphism design trends
- **API**: ExchangeRate API (https://www.exchangerate-api.com/)
- **Icons**: Custom designed glass aesthetic
- **Libraries**: Java Swing, Maven, MySQL JDBC

---

## 📞 Support

## ❤️ Support / Contact

If you like this project and want to support development, follow me on X (Twitter):  
**[@Piyush_legionP](https://x.com/Piyush_legionP)**

Feel free to report issues, suggest features, or just say hi!

### Getting Help
- **Issues**: Check [GitHub Issues](https://github.com/guguluP/Calculator-Currency-Converter/issues)
- **Discussions**: Start a [GitHub Discussion](https://github.com/guguluP/Calculator-Currency-Converter/discussions)
- **Documentation**: See docs folder
- **FAQ**: Common questions answered below

### FAQ

**Q: Can I use this without MySQL?**  
A: Yes! The app works perfectly without MySQL. Database is optional for history persistence.

**Q: Does it work offline?**  
A: Yes for calculations, but currency conversion needs internet connection.

**Q: How often are exchange rates updated?**  
A: Cached for 5 minutes to reduce API calls. Rates update in real-time when cache expires.

**Q: Can I export history?**  
A: Yes, through the History dialog. CSV export coming in v2.2.

**Q: What Java version is required?**  
A: Java 21 or higher. Download from oracle.com or adoptopenjdk.org.

---

## 📊 Repository Stats

- **Total Commits**: 33+
- **Active Development**: Yes
- **Last Updated**: May 2026
- **Contributors**: 1
- **Issues**: 0 (well-maintained!)

---

## 🌟 Support This Project

If you find this project useful:
- ⭐ **Star** the repository
- 🔄 **Fork** it for your use case
- 💬 **Discuss** features and ideas
- 🐛 **Report** bugs you find
- 🚀 **Contribute** improvements

---

<div align="center">

### Made with ❤️ by [guguluP](https://github.com/guguluP)

**Give a ⭐ if you like this project!**

[⬆ back to top](#-glasscalculator---modern-glassmorphism-scientific-calculator--currency-converter)

</div>
