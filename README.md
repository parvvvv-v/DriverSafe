# DriverSafe: Real-Time Drowsiness Detection & Driver Safety
DriverSafe is an AI-powered mobile application designed to prevent road accidents by monitoring driver alertness in real-time. Using computer vision and machine learning, the app detects signs of fatigue and distraction, providing immediate alerts to keep drivers focused on the road.

🛠 Key Features
Drowsiness Detection: Analyzes facial landmarks to monitor Eye Aspect Ratio (EAR) and detecting prolonged eye closure.

Yawn Sensing: Tracks mouth movements to identify frequent yawning as a secondary indicator of fatigue.

Real-Time Alerts: Triggers audible alarms and visual warnings instantly when potential drowsiness is detected.

Low-Latency Processing: Optimized for mobile hardware to ensure seamless, high-frame-rate monitoring without heavy battery drain.

Driver Statistics: (Optional) Provides a summary of alertness levels over the duration of the trip.

💻 Tech Stack
Frontend: XML for responsive UI design and layout management.

Backend: Kotlin/Java for robust application logic and camera integration.

Machine Learning: TensorFlow Lite for high-performance, on-device inference.

Computer Vision: MediaPipe or OpenCV for precise facial landmark detection and tracking.

🚀 How It Works
Face Tracking: The app utilizes the front-facing camera to map critical facial coordinates.

Feature Extraction: Mathematical models calculate the distance between eyelids and the opening of the mouth.

Threshold Analysis: If the eyelids remain closed or the mouth remains open beyond a specific temporal threshold, the system flags an event.

Immediate Feedback: The application engages the mobile device's notification system and speakers to alert the driver.

📦 Installation & Setup
Clone the repository to your local machine.

Open the project in Android Studio.

Ensure the latest NDK and CMake are installed for any native processing requirements.

Sync the project with Gradle files.

Deploy to a physical Android device (recommended for camera testing).

🤝 Contributing
Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are greatly appreciated.

Fork the Project

Create your Feature Branch

Commit your Changes

Push to the Branch

Open a Pull Request
