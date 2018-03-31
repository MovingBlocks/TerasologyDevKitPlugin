package org.terasology.plugin.event

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import com.intellij.psi.util.PsiTypesUtil
import org.jetbrains.uast.getContainingClass

class EventMarkerProvider : RelatedItemLineMarkerProvider() {
    companion object {
        val IMPORT_ICON = IconLoader.getIcon("/toolbarDecorator/import.png")
        val EXPORT_ICON = IconLoader.getIcon("/toolbarDecorator/export.png")
        val LIGHTNING_ICON = IconLoader.getIcon("/actions/lightning.png")
        val EmptyList = listOf<PsiElement>()
    }


    override fun collectNavigationMarkers(element: PsiElement, result: MutableCollection<in RelatedItemLineMarkerInfo<PsiElement>>) {
        if (!element.project.isValidTerasologyProject())
            return
        if (element is PsiFile || EventUtil.baseEventPsiClass == null) {
            EventUtil.scanProject(element.project)
            return
        }

        //process Event Declarer
        if (element is PsiClass && element.isInheritor(EventUtil.baseEventPsiClass!!, true)/*element.implementsList?.referenceElements?.find { it.resolve() == EventUtil.baseEventPsiClass } != null*/) {
            val handlersList = EventUtil.eventsTable[element]?.handlerRecords?.map { it.method } ?: EmptyList
            if (handlersList.isNotEmpty())
                result.add(NavigationGutterIconBuilder.create(IMPORT_ICON)
                        .setPopupTitle("Terasology Event Handler")
                        .setTooltipText("goto ${element.name} Handlers")
                        .setTargets(handlersList)

                        .createLineMarkerInfo(element.nameIdentifier!!))


            val tirggererslist = EventUtil.eventsTable[element]?.triggererRecords?.map { it.triggerer }
                    ?: EmptyList
            if (tirggererslist.isNotEmpty())
                result.add(NavigationGutterIconBuilder.create(EXPORT_ICON)
                        .setPopupTitle("Terasology Event Triggerer")
                        .setTooltipText("goto ${element} Triggerers")
                        .setTargets(tirggererslist)
                        .setCellRenderer(MethodInvokerRender())
                        .createLineMarkerInfo(element.nameIdentifier!!))
        }


        //process Event Receiver
        if (element is PsiMethod && element.annotations.find { it.nameReferenceElement!!.resolve() == EventUtil.receiveEventAnnotationsPsiClass } != null) {
            val targetEvent = element.parseEventHandlerRecord()!!.targetEvent
            result.add(NavigationGutterIconBuilder.create(LIGHTNING_ICON)
                    .setPopupTitle("Terasology Event Declaration")
                    .setTooltipText("goto ${targetEvent.name} Declaration")
                    .setTargets(targetEvent)
                    .createLineMarkerInfo(element.nameIdentifier!!))


            val handlersList = EventUtil.eventsTable[targetEvent]?.handlerRecords?.map { it.method }?.filter { it != element }
                    ?: EmptyList
            if (handlersList.isNotEmpty())
                result.add(NavigationGutterIconBuilder.create(IMPORT_ICON)
                        .setPopupTitle("Terasology Event Handler")
                        .setTooltipText("check other ${targetEvent.name} Handlers")
                        .setTargets(handlersList)
                        .createLineMarkerInfo(element.nameIdentifier!!))

            val tirggererslist = EventUtil.eventsTable[targetEvent]?.triggererRecords?.map { it.triggerer } ?: EmptyList
            if (tirggererslist.isNotEmpty())
                result.add(NavigationGutterIconBuilder.create(EXPORT_ICON)
                        .setPopupTitle("Terasology Event Triggerer")
                        .setTooltipText("goto ${targetEvent.name} Triggerers")
                        .setTargets(tirggererslist)
                        .setCellRenderer(MethodInvokerRender())
                        .createLineMarkerInfo(element.nameIdentifier!!))
        }

        //process Event Triggerer
        if (element is PsiReferenceExpression && element.resolve() == EventUtil.entityRefSendPsiMethod) {
            val event = PsiTypesUtil.getPsiClass((element.parent as PsiMethodCallExpressionImpl).argumentList.expressionTypes[0])!!

            result.add(NavigationGutterIconBuilder.create(LIGHTNING_ICON)
                    .setPopupTitle("Terasology Event Declaration")
                    .setTooltipText("goto ${event.name} Declaration")
                    .setTargets(event)
                    .createLineMarkerInfo(element))

            val handlersList = EventUtil.eventsTable[event]?.handlerRecords?.map { it.method }
                    ?: EmptyList
            if (handlersList.isNotEmpty())
                result.add(NavigationGutterIconBuilder.create(IMPORT_ICON)
                        .setPopupTitle("Terasology Event Handler")
                        .setTooltipText("goto ${event.name} Handlers")
                        .setTargets(handlersList)
                        .createLineMarkerInfo(element))
//            result.add(NavigationGutterIconBuilder.create(EXPORT_ICON)
//                    .setPopupTitle("Terasology Event Triggerer")
//                    .setTooltipText("check other ${targetEvent.name} Triggerers")
//                    .setTargets(EventUtil.eventsTable[targetEvent]?.triggererRecords?.map { it.triggerer }?: EmptyList)
//                    .createLineMarkerInfo(element.nameIdentifier!!))
        }

    }
}

class MethodInvokerRender : DefaultPsiElementCellRenderer() {
    override fun getContainerText(element: PsiElement, name: String?): String? {
        if(element is NavigationItem && element.presentation ==null){
            val containingClass = element.getContainingClass()
            if(containingClass !=null) {
                //val packagepsi = JavaPsiFacade.getInstance(element.project).findPackage(javaFile.getPackageName())
                val caller = element.findParent<PsiMethod>()
                return "("+  (if(caller==null)"" else "at ${caller.name}(${caller.parameterList.parameters.map {
                    it.typeElement!!.innermostComponentReferenceElement?.referenceName?:(it!!.typeElement!!.type as PsiPrimitiveType).name
                }.joinToString()})") +  " in ${(containingClass.containingFile as PsiJavaFile).packageName}.${containingClass.name})"
            }
        }
        return super.getContainerText(element, name)


    }
}

inline fun <reified P>PsiElement.findParent():P? {
    var parent = this.parent
    while(parent !is P && parent != null){
        parent = parent.parent
    }
    return parent as P?
}
