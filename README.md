# FoodieBuddy
A private Android app for close friends, where we can share everyday food recipes, manage ingredients inventory, groceries, and meal prep.

## Features
- Account creation using a Google account -> user data managed on Firebase Database
- General settings such as account management, language (EN and FR supported), display mode, ...
- Viewing all recipes published by other users, with many filters to choose from to narrow down the search (key-words, tags, creator, favourites, ...)
- Viewing other users' profiles
- Creating and editing your own recipes
- Managing your own ingredients inventory, with customizable categories
- Managing your shopping list: manually add items, batch-add items from a recipe (you can filter out the ones already in your inventory)
- Transfer bought ingredients from your shopping list to your inventory
- Meal-prep: set up your week's recipes, plan your groceries accordingly, set-up reminders to shop or cook during the week
- Add personal, private notes on recipes that aren't yours, if you want to remember to tune something to your preference
- Convert any recipe into a PDF file for downloading or printing


## Development
### Log tags
There are different logcat tags to help with debugging:
- Compose : When new screens are successfully composed.
- Debug : Only to use when currently debugging a specific feature. There shouldn't be any Debug tag on stable versions.
- Error : for errors correctly caught and handled.
- Login : All authentication, account existence checks / creation / accesses / deletion.
- MyDB : Successes and failures in accessing/updating the Firestore Database.
- NavAction : Navigation from a route to the other, backstack controls when going back in navigation history.
- RecipeVM : All RecipeViewModel actions like init, DB accesses, etc...
- UserVM : All UserViewModel actions like initialization, accesses to Database, etc...
  
### JavaDoc
All functions are commented with typical JavaDoc. To write them more easily, you can follow these steps:
- Go to Settings > Editor > Live Templates
- Click the + to create a new template
- Write what you want for the Abbreviation (e.g. funcDoc) and add a Description if you want
- Copy the following in the Template text:
```
/**
 *
 * 
 * @param 
 * @param 
 * @param 
 * @return
 */
```

- Click Define and tick Kotlin
- Now when you type the Abbreviation in your code and hit Tab, the JavaDoc template will paste.
  
You can also follow these same steps for a class JavaDoc (e.g. classDoc):
```
/**
 * 
 *
 * @property 
 * @property 
 * @property 
 */
```
