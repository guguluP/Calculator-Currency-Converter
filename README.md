# Calculator & Currency Converter

A full-featured calculator application with scientific mode and live currency conversion capabilities, built for macOS.

## Features

✨ **Core Features:**
- 🧮 **Standard Calculator Mode** - Basic arithmetic operations (addition, subtraction, multiplication, division)
- 🔬 **Scientific Mode** - Advanced mathematical functions including:
  - Power operations (x², x³, xʸ, 2ˣ)
  - Root functions (√, ³√, ʸ√)
  - Trigonometric functions (sin, cos, tan)
  - Logarithmic functions (log₁₀, log₂, logᵧ)
  - Inverse functions (1/x, sin⁻¹, cos⁻¹, tan⁻¹)
  - Hyperbolic functions (sinh, cosh, tanh)
  - Additional operations (factorial, parentheses, e constant)
- 💱 **Live Currency Conversion** - Real-time exchange rates with support for multiple currencies
- 📊 **History Tracking** - Maintains separate calculation and conversion history
- ⌨️ **Keyboard Shortcuts** - Full keyboard support for quick operations

## Screenshots

### Standard Calculator Interface
![Standard Calculator](Screenshots/calculator_standard.png)
*Main calculator view with basic arithmetic operations and standard functions*

### Scientific Calculator Mode
![Scientific Calculator](Screenshots/calculator_scientific.png)
*Extended calculator with advanced mathematical functions including trigonometric, logarithmic, and exponential operations*

### Currency Converter Integration
![Currency Converter](Screenshots/currency_converter.png)
*Live currency conversion with real-time exchange rates - showing INR conversion capability*

### Calculation History
![Calculation History](Screenshots/history_calculations.png)
*View all previous calculations with detailed results*

### Conversion History
![Conversion History](Screenshots/history_conversions.png)
*Track all currency conversions including CHF, AUD, and USD to INR conversions*

### Keyboard Shortcuts Reference
![Keyboard Shortcuts](Screenshots/keyboard_shortcuts.png)
*Complete keyboard shortcut guide for efficient calculator operation*

## Installation

### Requirements
- macOS 10.13 or later
- Python 3.8+

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/guguluP/Calculator-Currency-Converter.git
   cd Calculator-Currency-Converter
   ```

2. **Install dependencies**
   ```bash
   pip install -r requirements.txt
   ```

3. **Run the application**
   ```bash
   python main.py
   ```

Or use the pre-built macOS app (if available in releases):
- Download the latest `.app` file from [Releases](https://github.com/guguluP/Calculator-Currency-Converter/releases)
- Drag to Applications folder
- Launch from Applications

## Usage

### Basic Calculator
1. Launch the application
2. Enter numbers using the number buttons or keyboard
3. Select an operation (+, -, ×, ÷)
4. Press = or Enter to calculate

### Scientific Mode
1. Click the mode toggle or access from the menu
2. Use additional buttons for advanced functions:
   - **Powers & Roots**: x², x³, √, etc.
   - **Trigonometry**: sin, cos, tan (in DEG/RAD modes)
   - **Logarithms**: log₁₀, log₂
   - **Other**: Factorial (!), e constant

### Currency Conversion
1. Access the currency converter tab in History
2. Enter amount in the source currency
3. Select target currency
4. View real-time conversion results
5. All conversions are saved in history

### Keyboard Shortcuts

| Key(s) | Action |
|--------|--------|
| **Enter** or **=** | Evaluate expression |
| **Backspace** | Delete last character |
| **Escape** or **Delete** | Clear (AC) |
| **Ctrl + C** | Copy |
| **Ctrl + V** | Paste |
| **^** | Power |
| **%** | Percent |
| **!** | Factorial (Scientific) |
| **Ctrl + A** | Select All |
| **Right-click** | Context menu |

## Features in Detail

### History Management
- **Automatic Saving**: All calculations and conversions are saved automatically
- **Persistent Storage**: Uses MySQL database and preferences
- **Separate Tabs**: View calculations and conversions independently
- **Clear Options**: Clear specific category or all history

### Live Currency Rates
- Real-time exchange rate updates
- Support for major currencies (CHF, AUD, USD, INR, etc.)
- Status indicator showing "Live rates - Ready"
- Cached rates for offline functionality

### User Interface
- Dark mode optimized interface
- Responsive button layout
- Large, easy-to-read display
- Mode indicator showing current currency or operation

## Data Storage

All history entries are saved in:
```
Preferences + MySQL Database
```

You can manage your data through:
- Clear Calculations
- Clear Conversions
- Clear All

## Supported Currencies

- INR (Indian Rupee) - Default
- USD (US Dollar)
- AUD (Australian Dollar)
- CHF (Swiss Franc)
- And many more international currencies

## Troubleshooting

### History not saving?
- Check database connection
- Verify write permissions in preferences folder

### Currency rates not updating?
- Check internet connection
- Rates may be cached from last update
- Try closing and reopening the app

### Keyboard shortcuts not working?
- Ensure calculator window is in focus
- Check system keyboard preferences

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Author

Created by [guguluP](https://github.com/guguluP)

## Support

If you encounter any issues or have suggestions:
- Open an [Issue](https://github.com/guguluP/Calculator-Currency-Converter/issues)
- Check existing issues for solutions

---

**Last Updated**: May 2026
