package org.terasology.plugin.event

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.*

class EventMarkerProvider: RelatedItemLineMarkerProvider() {
    companion object {
        val IMPORT_ICON = IconLoader.getIcon("/toolbarDecorator/import.png")
        val EXPORT_ICON = IconLoader.getIcon("/toolbarDecorator/export.png")
        val LIGHTNING_ICON = IconLoader.getIcon("/actions/lightning.png")
        val EmptyList = listOf<PsiElement>()
    }



    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>) {
        if(!element.project.isValidTerasologyProject())
            return
        if (element is PsiFile || EventUtil.baseEventPsiClass == null){
            EventUtil.scanProject(element.project)
            return
        }

        //process Event Declarer
        if (element is PsiClass && element.isInheritor(EventUtil.baseEventPsiClass!!,true)/*element.implementsList?.referenceElements?.find { it.resolve() == EventUtil.baseEventPsiClass } != null*/)
            result.add(NavigationGutterIconBuilder.create(IMPORT_ICON)
                    .setPopupTitle("Terasology Event Handler")
                    .setTooltipText("goto Event Handler")
                    .setTargets(EventUtil.eventsTable[element]?.handlerRecords?.map { it.method }?: EmptyList)
                    .createLineMarkerInfo(element.nameIdentifier!!))


        //process Event Receiver
        if(element is PsiMethod && element.annotations.find{it.nameReferenceElement!!.resolve() == EventUtil.receiveEventAnnotationsPsiClass } != null) {
            result.add(NavigationGutterIconBuilder.create(LIGHTNING_ICON)
                    .setPopupTitle("Terasology Event Declarer")
                    .setTooltipText("goto Event Declarer")
                    .setTargets(element.parseEventHandlerRecord()!!.targetEvent)
                    .createLineMarkerInfo(element.nameIdentifier!!))

            result.add(NavigationGutterIconBuilder.create(IMPORT_ICON)
                    .setPopupTitle("Terasology Event Handler")
                    .setTooltipText("goto other Event Handler")
                    .setTargets(EventUtil.eventsTable[element.parseEventHandlerRecord()!!.targetEvent]?.handlerRecords?.map { it.method }?: EmptyList)
                    .createLineMarkerInfo(element.nameIdentifier!!))
        }
    }
}