import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.page').then(m => m.DashboardPage),
  },
  {
    path: 'explorer',
    loadComponent: () =>
      import('./features/trade-explorer/trade-explorer.page').then(m => m.TradeExplorerPage),
  },
  {
    path: 'status',
    loadComponent: () =>
      import('./features/status/status.page').then(m => m.StatusPage),
  },
  {
    path: 'reports',
    loadComponent: () =>
      import('./features/reports/reports.page').then(m => m.ReportsPage),
  },
];
