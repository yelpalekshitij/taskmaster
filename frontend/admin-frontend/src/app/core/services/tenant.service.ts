import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Tenant, TenantPage, CreateTenantRequest, TenantStats } from '../models/admin.model';

@Injectable({ providedIn: 'root' })
export class TenantService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/api/v1/tenants`;

  getTenants(page = 0, size = 20, search = '', activeOnly?: boolean): Observable<TenantPage> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);

    if (search) params = params.set('search', search);
    if (activeOnly !== undefined) params = params.set('active', activeOnly);

    return this.http.get<TenantPage>(this.baseUrl, { params });
  }

  getTenant(id: string): Observable<Tenant> {
    return this.http.get<Tenant>(`${this.baseUrl}/${id}`);
  }

  createTenant(request: CreateTenantRequest): Observable<Tenant> {
    return this.http.post<Tenant>(this.baseUrl, request);
  }

  updateTenant(id: string, updates: Partial<Tenant>): Observable<Tenant> {
    return this.http.put<Tenant>(`${this.baseUrl}/${id}`, updates);
  }

  activateTenant(id: string): Observable<Tenant> {
    return this.http.patch<Tenant>(`${this.baseUrl}/${id}/activate`, {});
  }

  deactivateTenant(id: string): Observable<Tenant> {
    return this.http.patch<Tenant>(`${this.baseUrl}/${id}/deactivate`, {});
  }

  getTenantStats(id: string): Observable<TenantStats> {
    return this.http.get<TenantStats>(`${this.baseUrl}/${id}/stats`);
  }
}
