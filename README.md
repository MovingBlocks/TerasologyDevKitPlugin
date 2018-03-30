Terasology Developer Kit Plugin
==
This plugin is build to help developing with Terasology


## ToDo feature
### RelatedItemProvider
- [x] Finding receiver function  
    - [ ] Separately marking possible receiver ,and receiver which will definitely receive the event   
    - [ ] Sorting by priority and mark the non default priority event in related item list
- [x] Goto event declarer from receiver
- [ ] Finding Event possible Sender location  
    - [ ] Entity.send()  
    - [ ] Entity.saveComponent()  
    - [ ] Consider the entity build with prefab.(Need to search the prefab resource)  
- [ ] The code which create entity via prefab goto prefab file.
- [ ] Prefab to related Component
### Detect incorrect usage 
- [ ] Add a event receiver to listen a event marked with @ServerEvent on client system
- [ ] Prefab that has a nonexisting Component class or invlid name
- [ ] Invalid event receive method parameter
### Template
- [ ] Event
    - [ ] Can select event type (server event ,etc.)
- [ ] Prefab
    - [ ] Can select extend parent
    - [ ] Can select components from existing
- [ ] Module (replace gradle command)
### AutoComplete
- [ ] Prefab finding existing Component class
- [ ] Prefab parent finding existing prefab

### Custon icon
- [ ] EventReceiver
- [ ] EvnetSender
- [ ] Prefab
- [ ] Component
### Other
- [ ] Rebuild project into Gradle version for integration with Jenkins

## Developing
### Requirement
- Intellij idea
- Plugin DevKit (1.0) Plugin
- Kotlin Plugin
- Intellij Community Source SDK `git://git.jetbrains.org/idea/community.git`
Need Plugin DevKit (1.0) and Kotlin plugin install and enabled.  
Than clone the project and simply import into Intellij Idea.
