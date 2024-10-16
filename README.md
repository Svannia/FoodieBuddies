# FoodieBuddy
A private Android app for close friends, where we can share everyday food recipes, make shopping lists, ...

## Development
### Log tags
There are different logcat tags to help with debugging:
- Compose : When new screens are successfully composed.
- DB : Successes and failures in accessing/updating the Firestore Database.
- Debug : Only to use when currently debugging a specific feature. There shouldn't be any Debug tag on stable versions.
- Error : for errors correctly caught and handled.
- Login : All authentication, account existence checks / creation / accesses / deletion.
  
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
