import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { App } from './app';
import { Forecast } from './core/api/models/forecast';
import { Municipality } from './core/api/models/municipality';
import { WeatherApiService } from './core/api/weather-api.service';

const MADRID: Municipality = { code: '28079', name: 'Madrid' };
const BARCELONA: Municipality = { code: '08019', name: 'Barcelona' };
const FORECAST_CEL: Forecast = { mediaTemperatura: 25.5, unidadTemperatura: 'G_CEL', probPrecipitacion: [] };

describe('App', () => {
  let searchMunicipalities: ReturnType<typeof vi.fn>;
  let getForecast: ReturnType<typeof vi.fn>;
  let component: App;

  beforeEach(() => {
    searchMunicipalities = vi.fn().mockReturnValue(of([]));
    getForecast = vi.fn().mockReturnValue(of(FORECAST_CEL));

    TestBed.configureTestingModule({
      imports: [App],
      providers: [{ provide: WeatherApiService, useValue: { searchMunicipalities, getForecast } }],
    });

    component = TestBed.createComponent(App).componentInstance;
  });

  it('creates the app', () => {
    expect(component).toBeTruthy();
  });

  it('does not call getForecast in the initial state', () => {
    component.forecastState$.subscribe();

    expect(getForecast).not.toHaveBeenCalled();
  });

  it('searches municipalities after the user types, respecting the 300ms debounce', () => {
    vi.useFakeTimers();
    let results: Municipality[] | undefined;
    component.municipalities$.subscribe((value) => (results = value));

    component.municipalityControl.setValue('mad');
    expect(searchMunicipalities).not.toHaveBeenCalled();

    vi.advanceTimersByTime(300);

    expect(searchMunicipalities).toHaveBeenCalledWith('mad');
    expect(results).toEqual([]);
    vi.useRealTimers();
  });

  it('does not call searchMunicipalities for empty/blank text and produces an empty list', () => {
    vi.useFakeTimers();
    let results: Municipality[] | undefined;
    component.municipalities$.subscribe((value) => (results = value));

    component.municipalityControl.setValue('   ');
    vi.advanceTimersByTime(300);

    expect(searchMunicipalities).not.toHaveBeenCalled();
    expect(results).toEqual([]);
    vi.useRealTimers();
  });

  it('requests the forecast in G_CEL when a municipality is selected', () => {
    component.forecastState$.subscribe();

    component.municipalityControl.setValue(MADRID);

    expect(getForecast).toHaveBeenCalledWith('28079', 'G_CEL');
  });

  it('requests the forecast again in G_FAH when the unit changes with a municipality selected', () => {
    component.forecastState$.subscribe();
    component.municipalityControl.setValue(MADRID);
    getForecast.mockClear();

    component.temperatureUnitControl.setValue('G_FAH');

    expect(getForecast).toHaveBeenCalledWith('28079', 'G_FAH');
  });

  it('does not call getForecast when the unit changes without a selected municipality', () => {
    component.forecastState$.subscribe();

    component.temperatureUnitControl.setValue('G_FAH');

    expect(getForecast).not.toHaveBeenCalled();
  });

  it('requests the forecast for the new municipality when the selection changes', () => {
    component.forecastState$.subscribe();
    component.municipalityControl.setValue(MADRID);
    getForecast.mockClear();

    component.municipalityControl.setValue(BARCELONA);

    expect(getForecast).toHaveBeenCalledWith('08019', 'G_CEL');
  });

  it('stops using the previous municipality code after the text is edited manually', () => {
    component.forecastState$.subscribe();
    component.municipalityControl.setValue(MADRID);
    getForecast.mockClear();

    component.municipalityControl.setValue('mad');

    expect(getForecast).not.toHaveBeenCalled();
  });

  it('keeps future municipality searches working after a search error', () => {
    vi.useFakeTimers();
    searchMunicipalities.mockReturnValueOnce(throwError(() => new Error('boom')));
    let results: Municipality[] | undefined;
    component.municipalities$.subscribe((value) => (results = value));

    component.municipalityControl.setValue('mad');
    vi.advanceTimersByTime(300);
    expect(results).toEqual([]);

    searchMunicipalities.mockReturnValue(of([MADRID]));
    component.municipalityControl.setValue('madr');
    vi.advanceTimersByTime(300);

    expect(results).toEqual([MADRID]);
    vi.useRealTimers();
  });

  it('exposes an error state after a forecast failure and recovers on the next update', () => {
    getForecast.mockReturnValueOnce(throwError(() => new Error('boom')));
    const states: { loading: boolean; forecast: Forecast | null; error: boolean }[] = [];
    component.forecastState$.subscribe((state) => states.push(state));

    component.municipalityControl.setValue(MADRID);
    expect(states.at(-1)).toEqual({ loading: false, forecast: null, error: true });

    getForecast.mockReturnValue(of(FORECAST_CEL));
    component.municipalityControl.setValue(BARCELONA);

    expect(states.at(-1)).toEqual({ loading: false, forecast: FORECAST_CEL, error: false });
  });

  it('displayWith shows municipality.name and nothing for free text', () => {
    expect(component.displayMunicipality(MADRID)).toBe('Madrid');
    expect(component.displayMunicipality('some text')).toBe('');
    expect(component.displayMunicipality(null)).toBe('');
  });
});
