import {
  createRootRoute,
  createRoute,
  createRouter,
  RouterProvider,
} from '@tanstack/react-router'
import { AppLayout } from '../components/AppLayout'
import { DataExportPage } from '../pages/DataExportPage'
import { ExperimentPage } from '../pages/ExperimentPage'
import { KnowledgeGraphPage } from '../pages/KnowledgeGraphPage'
import { KnowledgePage } from '../pages/KnowledgePage'
import { LibraryPage } from '../pages/LibraryPage'
import { PlaceholderPage } from '../pages/PlaceholderPage'
import { MemoryPage } from '../pages/MemoryPage'
import { ReflectionPage } from '../pages/ReflectionPage'
import { SearchPage } from '../pages/SearchPage'
import { TodayPage } from '../pages/TodayPage'
import { TransformationDetailPage } from '../pages/TransformationDetailPage'
import { TransformationsPage } from '../pages/TransformationsPage'
import { WisdomPage } from '../pages/WisdomPage'

const rootRoute = createRootRoute({
  component: AppLayout,
})

const todayRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/today',
  component: TodayPage,
})

const homeRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: TodayPage,
})

const transformationsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/transformations',
  component: TransformationsPage,
})

const transformationDetailRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/transformations/$id',
  component: TransformationDetailPage,
})

const experimentRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/experiments/$id',
  component: ExperimentPage,
})

const reflectionRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/reflections/$id',
  component: ReflectionPage,
})

const wisdomRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/wisdom',
  component: WisdomPage,
})

const libraryRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/library',
  component: LibraryPage,
})

const searchRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/search',
  component: SearchPage,
})

const knowledgeRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/knowledge',
  component: KnowledgePage,
})

const knowledgeGraphRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/knowledge-graph/$nodeType/$sourceRecordId',
  component: KnowledgeGraphPage,
})

const settingsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/settings',
  component: () => <PlaceholderPage title="Settings" />,
})

const settingsPrivacyRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/settings/privacy',
  component: () => <PlaceholderPage title="Privacy" />,
})

const settingsAiRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/settings/ai',
  component: () => <PlaceholderPage title="AI Settings" />,
})

const settingsMemoryRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/settings/memory',
  component: MemoryPage,
})

const settingsExportRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/settings/export',
  component: DataExportPage,
})

const routeTree = rootRoute.addChildren([
  homeRoute,
  todayRoute,
  transformationsRoute,
  transformationDetailRoute,
  experimentRoute,
  reflectionRoute,
  wisdomRoute,
  libraryRoute,
  searchRoute,
  knowledgeRoute,
  knowledgeGraphRoute,
  settingsRoute,
  settingsPrivacyRoute,
  settingsAiRoute,
  settingsMemoryRoute,
  settingsExportRoute,
])

const router = createRouter({ routeTree })

export function AppRouter() {
  return <RouterProvider router={router} />
}
