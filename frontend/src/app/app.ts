import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { Observable, combineLatest, of } from 'rxjs';
import { catchError, debounceTime, distinctUntilChanged, filter, map, startWith, switchMap } from 'rxjs/operators';
import { Forecast } from './core/api/models/forecast';
import { Municipality } from './core/api/models/municipality';
import { TemperatureUnit } from './core/api/models/temperature-unit';
import { WeatherApiService } from './core/api/weather-api.service';

const SEARCH_DEBOUNCE_MS = 300;

interface ForecastState {
  loading: boolean;
  forecast: Forecast | null;
  error: boolean;
}

const IDLE_STATE: ForecastState = { loading: false, forecast: null, error: false };
const LOADING_STATE: ForecastState = { loading: true, forecast: null, error: false };
const ERROR_STATE: ForecastState = { loading: false, forecast: null, error: true };

@Component({
  selector: 'app-root',
  imports: [
    AsyncPipe,
    ReactiveFormsModule,
    MatAutocompleteModule,
    MatButtonToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './app.scss',
})
export class App {
  private readonly weatherApi = inject(WeatherApiService);

  readonly municipalityControl = new FormControl<string | Municipality>('', { nonNullable: true });
  readonly temperatureUnitControl = new FormControl<TemperatureUnit>('G_CEL', { nonNullable: true });

  readonly municipalities$: Observable<Municipality[]> = this.municipalityControl.valueChanges.pipe(
    filter((value): value is string => typeof value === 'string'),
    map((value) => value.trim()),
    debounceTime(SEARCH_DEBOUNCE_MS),
    distinctUntilChanged(),
    switchMap((query) =>
      query
        ? this.weatherApi.searchMunicipalities(query).pipe(catchError(() => of<Municipality[]>([])))
        : of<Municipality[]>([]),
    ),
  );

  private readonly selectedMunicipality$: Observable<Municipality | null> = this.municipalityControl.valueChanges.pipe(
    startWith(this.municipalityControl.value),
    map((value) => (typeof value === 'string' ? null : value)),
  );

  private readonly temperatureUnit$: Observable<TemperatureUnit> = this.temperatureUnitControl.valueChanges.pipe(
    startWith(this.temperatureUnitControl.value),
  );

  readonly forecastState$: Observable<ForecastState> = combineLatest([
    this.selectedMunicipality$,
    this.temperatureUnit$,
  ]).pipe(
    switchMap(([municipality, temperatureUnit]) => {
      if (!municipality) {
        return of(IDLE_STATE);
      }
      return this.weatherApi.getForecast(municipality.code, temperatureUnit).pipe(
        map((forecast): ForecastState => ({ loading: false, forecast, error: false })),
        startWith(LOADING_STATE),
        catchError(() => of(ERROR_STATE)),
      );
    }),
  );

  readonly displayMunicipality = (municipality: Municipality | string | null): string =>
    municipality && typeof municipality === 'object' ? municipality.name : '';

  protected temperatureSymbol(unit: TemperatureUnit): string {
    return unit === 'G_FAH' ? '°F' : '°C';
  }
}
