# Meal Planner 🍽️

![Meal Planner Banner](https://your-image-link.com/banner.png)  
_A complete solution for meal planning and recipe discovery._

## 📌 Overview
Meal Planner is an Android application designed to help users plan their weekly meals, discover new recipes, and save favorite meals for easy access, even without an internet connection. The app integrates Firebase for authentication and data synchronization while leveraging Room Database for local storage.

## ✨ Features
- **Meal of the Day** – Get inspired with a random meal suggestion.
- **Advanced Search** – Find meals by category, country, or ingredients.
- **Meal Categories & Countries** – Explore meals from around the world.
- **Favorites** – Save meals and access them offline.
- **Weekly Meal Planning** – Add meals to your weekly plan and track them.
- **Offline Mode** – View your favorite meals and weekly plans without the internet.
- **User Authentication** – Sign up/login using email, Google, Facebook, or Twitter.
- **Data Synchronization** – Automatically sync and restore data upon login.
- **Guest Mode** – Allows non-registered users to browse and search meals.

## 🛠️ Tech Stack
- **Language:** Kotlin
- **Architecture:** MVVM (Model-View-ViewModel)
- **Networking:** Retrofit (for API calls)
- **Database:** Room Database (for local storage)
- **Authentication & Syncing:** Firebase Authentication & Firestore
- **State Management:** LiveData & ViewModel
- **Storage:** SharedPreferences
- **UI Design:** Material Design Components

## 🚀 Installation & Setup
1. **Clone the repository:**
   ```sh
   git clone https://github.com/MoamenAbdelrhman/Meal-Planner.git
   ```
2. **Open the project** in Android Studio.
3. **Set up Firebase:**
   - Add the `google-services.json` file in `app/`.
   - Enable Firebase Authentication (Email, Google, Facebook, Twitter).
4. **Run the application** on an emulator or a physical device.

## 📸 Screenshots

<table>
  <tr>
    <td align="center"><b>🏠 Home Screen</b></td>
    <td align="center"><b>🔍 Meal Search</b></td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/MoamenAbdelrhman/Meal-Planner/blob/master/WhatsApp%20Image%202025-03-26%20at%2016.27.08_3f3a34d7.jpg" width="45%"/></td>
    <td align="center"><img src="https://github.com/MoamenAbdelrhman/Meal-Planner/blob/master/WhatsApp%20Image%202025-03-26%20at%2016.27.09_48ac8e48.jpg" width="45%"/></td>
  </tr>
  <tr>
    <td align="center"><b>🍽️ Meal Details</b></td>
    <td align="center"><b>🗓️ Meal Plan</b></td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/MoamenAbdelrhman/Meal-Planner/blob/master/photo_2025-03-26_17-53-12.jpg" width="45%"/></td>
    <td align="center"><img src="https://github.com/MoamenAbdelrhman/Meal-Planner/blob/master/WhatsApp%20Image%202025-03-26%20at%2017.35.08_1b268352.jpg" width="45%"/></td>
  </tr>
  <tr>
    <td align="center"><b>❤️ Meal Favorites</b></td>
  </tr>
  <tr>
    <td align="center"><img src="https://github.com/MoamenAbdelrhman/Meal-Planner/blob/master/WhatsApp%20Image%202025-03-26%20at%2017.35.08_dec4fe1e.jpg" width="45%"/></td>
  </tr>
</table>


## 📚 API Reference
The app fetches meal data from [TheMealDB API](https://themealdb.com/api.php).

## 🏗️ Project Structure
```
MealPlanner/
│── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/mealplanner/
│   │   │   │   ├── ui/          # UI Components
│   │   │   │   ├── data/        # Data & API Handling
│   │   │   │   ├── viewmodel/   # ViewModels
│   │   │   │   ├── repository/  # Repository Pattern
│   │   │   ├── res/
│   │   │   │   ├── layout/      # XML Layouts
│   │   │   │   ├── drawable/    # Icons & Images
```

## 🤝 Contribution
Contributions are welcome! Feel free to open an issue or submit a pull request.

## 📜 License
This project is licensed under the [MIT License](LICENSE).

## 📩 Contact
📧 Email: moameny84@gmail.com  
🔗 LinkedIn: [Moamen Abdelrahman](https://www.linkedin.com/in/moamenabdelrhman/)
