1. Overview

The Smart Weather GUI is a Java desktop application that connects to the OpenWeatherMap API to display live weather conditions for any city in the world.
It uses the user’s input, calls the API through an HTTP request, parses the returned JSON using Gson, and displays a formatted weather report.

Key Features

Fetch live weather data by city name

Choose between Metric (°C) or Imperial (°F) units

View current temperature, feels-like value, humidity, wind speed, and weather description

Get automatic advice (e.g., “☔ Consider an umbrella”)

Save reports as text files

Simple and responsive Swing GUI

2. System Requirements

Java JDK 17 or newer

IntelliJ IDEA (recommended)

Gson Library (v2.11.0)

Add via File → Project Structure → Libraries → From Maven...

Search: com.google.code.gson:gson:2.11.0

3. How to Set Up

Open IntelliJ IDEA.

Create a new Java project and name it SmartWeatherGUI.

Inside the src folder, create a file called Main.java.

Paste the full code provided.

Confirm the Gson library is added.

Click Run ▶️ to launch the program.

4. How to Use the Application

When the program launches, you’ll see three buttons and text fields:

City Field – enter a location such as Sonora,US or Twain Harte,US.

Units Dropdown – choose between “metric” (°C) or “imperial” (°F).

Buttons:

Fetch Weather — calls the OpenWeatherMap API and displays a formatted report.

Save Report — saves the output to a file named weather_summary_YYYYMMDD_HHMMSS.txt.

Exit — closes the application.

Example usage:

Type San Francisco,US in the City box.

Select imperial for °F.

Click Fetch Weather.

Wait a moment — the current conditions will appear in the text area.

To save the report, click Save Report.
