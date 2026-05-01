import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { GlobalStats, TenantStats } from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/v1/reports`;

  getGlobalStats(): Observable<GlobalStats> {
    return this.http.get<GlobalStats>(`${this.baseUrl}/global`);
  }

  getAllTenantStats(): Observable<TenantStats[]> {
    return this.http.get<TenantStats[]>(`${this.baseUrl}/tenants`);
  }

  getTenantStats(tenantId: string): Observable<TenantStats> {
    return this.http.get<TenantStats>(`${this.baseUrl}/tenants/${tenantId}`);
  }
}
