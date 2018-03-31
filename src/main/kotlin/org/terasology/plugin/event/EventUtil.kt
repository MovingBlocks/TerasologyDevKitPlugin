package org.terasology.plugin.event

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.FileTypeIndex
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTypesUtil


object EventUtil {
    val eventsTable = HashMap<PsiClass, EventRecord>()
    var receiveEventAnnotationsPsiClass: PsiClass? = null
    var baseEventPsiClass: PsiClass? = null
    var entityRefSendPsiMethod: PsiMethod? = null

    fun scanProject(project: Project): Boolean {
        if (!project.isValidTerasologyProject())
            return false
        eventsTable.clear()

        receiveEventAnnotationsPsiClass = project.getReceiveEventAnnotationsPsiClass()
        baseEventPsiClass = project.getBaseEventPsiClass()
        entityRefSendPsiMethod = project.findPsiClass("org.terasology.entitySystem.entity.EntityRef")!!.findMethodsByName("send",false)[0]

        project.forEachAnnotated(receiveEventAnnotationsPsiClass!!) {
            if(it !is PsiMethod)
                return@forEachAnnotated
            val record = it.parseEventHandlerRecord()?:return@forEachAnnotated
            if(record.targetEvent !in eventsTable)
                eventsTable[record.targetEvent] = EventRecord(record.targetEvent)
            eventsTable[record.targetEvent]!!.handlerRecords.add(record)
        }


        MethodReferencesSearch.search(entityRefSendPsiMethod).forEach {
            if(it !is PsiReferenceExpression)
                return@forEach
            val event = PsiTypesUtil.getPsiClass((it.parent as PsiMethodCallExpressionImpl).argumentList.expressionTypes[0])!!
            if(event !in eventsTable)
                eventsTable[event] = EventRecord(event)
            eventsTable[event]!!.triggererRecords.add(EventTriggererRecord(it, setOf()))
        }

        return true
    }



}

data class EventRecord(val eventClass: PsiClass, val triggererRecords: MutableList<EventTriggererRecord> = mutableListOf(), val handlerRecords: MutableList<EventHandlerRecord> = mutableListOf())

data class EventTriggererRecord(val triggerer: PsiElement, val components: Set<PsiClass>)

data class EventHandlerRecord(val method: PsiMethod, val targetEvent: PsiClass, val components: Set<PsiClass>)


fun Project.isValidTerasologyProject() =
        JavaPsiFacade.getInstance(this).findPackage("org.terasology") != null

fun Project.findPsiClass(classname: String) =
        JavaPsiFacade.getInstance(this).findClass(classname, GlobalSearchScope.allScope(this))

fun Project.getAllEventPsiClass() =
        ClassInheritorsSearch.search(this.findPsiClass("org.terasology.entitySystem.event.Event")!!)

fun Project.getReceiveEventAnnotationsPsiClass() =
        findPsiClass("org.terasology.entitySystem.event.ReceiveEvent")

fun Project.getBaseEventPsiClass() =
        findPsiClass("org.terasology.entitySystem.event.Event")

fun Project.forEachAnnotated(clasz:PsiClass,action: (PsiMember) -> Unit) =
        AnnotatedMembersSearch.search(clasz)
                .forEach(action)


fun PsiMethod.parseEventHandlerRecord(): EventHandlerRecord? {
    val components = mutableSetOf<PsiClass>()
    var targetEventClass: PsiClass? = null

    //process annotation
    val annotation = this.annotations
            .find {
                it.nameReferenceElement!!.resolve() == EventUtil.receiveEventAnnotationsPsiClass
            } ?: return null
    annotation.parameterList.attributes
            .forEach {
                when (it.name) {
                    "components" -> {
                        val value = it.value
                        when (value) {
                            is PsiClassObjectAccessExpression -> components.add(value.operand.innermostComponentReferenceElement!!.resolve() as PsiClass)
                            is PsiArrayInitializerMemberValue -> value.initializers.forEach { ob -> components.add((ob as PsiClassObjectAccessExpression).operand.innermostComponentReferenceElement!!.resolve() as PsiClass) }
                        }
                    }
                }
            }


    //process parameter
    this.parameterList.parameters.withIndex()
            .forEach {
                (index , parameter )->
                when{
                    index == 0 -> targetEventClass = parameter.typeElement!!.innermostComponentReferenceElement!!.resolve() as PsiClass
                    index >= 2 ->  components.add(parameter.typeElement!!.innermostComponentReferenceElement!!.resolve() as PsiClass)
                }

            }
    if (targetEventClass == null)
        return null

    return EventHandlerRecord(this, targetEventClass!!, components.toSet())
}

inline fun <reified PSI_CLASS : PsiElement> Project.forEachPsiChildren(action: (PSI_CLASS) -> Unit) = FileBasedIndex.getInstance()
        .getContainingFiles<FileType, Void>(FileTypeIndex.NAME, JavaFileType.INSTANCE, GlobalSearchScope.projectScope(this))
        .map { PsiManager.getInstance(this).findFile(it) as PsiJavaFile }
        .flatMap { PsiTreeUtil.findChildrenOfType(it, PSI_CLASS::class.java) }
        .forEach(action)
