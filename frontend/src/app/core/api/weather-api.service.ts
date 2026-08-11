import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api.config';
import { Forecast } from './models/forecast';
import { Municipality } from './models/municipality';
import { TemperatureUnit } from './models/temperature-unit';

@Injectable({ providedIn: 'root' })
export class WeatherApiService {
  private readonly http = inject(HttpClient);

  searchMunicipalities(name: string): Observable<Municipality[]> {
    const params = new HttpParams().set('name', name);
    return this.http.get<Municipality[]>(`${API_BASE_URL}/municipalities`, { params });
  }

  getForecast(municipalityCode: string, temperatureUnit: TemperatureUnit): Observable<Forecast> {
    const params = new HttpParams().set('temperatureUnit', temperatureUnit);
    return this.http.get<Forecast>(`${API_BASE_URL}/forecast/${municipalityCode}`, { params });
  }
}
